import com.sun.source.tree.WhileLoopTree;

import javax.sound.sampled.AudioFileFormat;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.channels.*;

public class PrankServer {

    private HashMap<Integer, PrintWriter> outputs;
    private HashMap<Integer, BufferedReader> ins;
    private HashMap<Integer, Socket> sockets;
    private HashMap<Integer, String> names;
    private HashMap<Integer, List<String>> sounds;
    private List<Integer> ids;
    private List<String> filetypes;
    private String displayMsg;
    private Thread thread;
    private PrankServer() throws FileNotFoundException {
        outputs = new HashMap<>();
        sockets = new HashMap<>();
        ins = new HashMap<>();
        sounds = new HashMap<>();
        names = new HashMap<>();
        ids = new ArrayList<>();
        displayMsg = null;
        filetypes = new ArrayList<>();
        File file = new File("filetypes.txt");
        Scanner read = new Scanner(file);
        while (read.hasNext()) {
            filetypes.add(read.nextLine());
        }
    }

    private void serverStart() {
        Thread connect = new Thread(new GetConnections());
        thread = Thread.currentThread();
        connect.start();
        String input;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        updateDisplay(null, 1);
        while (true) {
            try {
                input = in.readLine();
                String[] args = input.split(" ");
                if (input.equals("exit")) {
                    System.exit(0);
                }
                else {
                    PrintWriter out;
                    int comp = -1;
                    if (args[0].equals("play") && (args.length == 3 || args.length == 4)) {
                        try {
                            comp = Integer.parseInt(args[args.length - 1]);
                            if (ids.contains(comp)) {
                                out = outputs.get(comp);
                                if (!sounds.get(comp).contains(args[1])) updateDisplay("\"" + args[1] + "\" is not a valid sound", 1);
                                else {
                                    updateDisplay(null, 1);
                                    out.print(args[0] + " " + args[1] + " ");
                                    if (args.length == 4) out.print(args[2]);
                                    out.println();
                                    out.flush();
                                }
                            }
                            else updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                        }
                        catch (NumberFormatException e) {
                            updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                        }

                    }
                    else if (args[0].equals("listsounds")) {
                        try {
                            comp = Integer.parseInt(args[args.length - 1]);
                            if (ids.contains(comp)) {
                                String list = "";
                                for (String temp : sounds.get(comp)) {
                                    list = list + temp + "\n";
                                }
                                updateDisplay(list, 1);
                            }
                            else updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                        }
                        catch (NumberFormatException e) {
                            updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                        }
                    }
                    else if (args[0].equals("file") && args.length >= 2) {
                        if (args[1].equals("add") && args.length >= 6) {
                            int length = 0;
                            if (filetypes.contains(args[2])) {
                                if (args[args.length - 2].equals("source")) {
                                    try {
                                        comp = Integer.parseInt(args[args.length - 1]);
                                        if (!sockets.containsKey(comp)) updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                                        else {
                                            OutputStream outputStream = sockets.get(comp).getOutputStream();
                                            String path = args[3];
                                            for (int i = 4; i < args.length - 2; i++) {
                                                path = path + " " + args[i];
                                            }
                                            File newSound = new File("files/" + path + "." + args[2]);
                                            if (!newSound.exists()) {
                                                updateDisplay("Could not find file", 1);
                                            }
                                            else {
                                                out = outputs.get(comp);
                                                byte[] bytes = new byte[32 * 1024];
                                                InputStream write = new FileInputStream(newSound);
                                                int count;
                                                while ((count = write.read(bytes)) > 0) length = length + count;
                                                path = "file add " + args[2] + " " + args[3];
                                                for (int i = 4; i < args.length - 2; i++) {
                                                    path = path + " " + args[i];
                                                }
                                                path = path + " " + args[args.length - 2] + " " + length;
                                                out.println(path);
                                                out.flush();
                                                write = new FileInputStream(newSound);
                                                while ((count = write.read(bytes)) > 0)  outputStream.write(bytes, 0, count);
                                                updateDisplay("File uploaded", 1);
                                            }
                                        }
                                    }
                                    catch (NumberFormatException e) {
                                        updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                                    }
                                }
                                else if (args[args.length - 2].equals("new")) {
                                    try {
                                        comp = Integer.parseInt(args[args.length - 1]);
                                        if (!sockets.containsKey(comp))
                                            updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                                        else {
                                            out = outputs.get(comp);
                                            String text = updateDisplay(null, 2);
                                            if (text == null) updateDisplay("Error with I/O", 1);
                                            else {
                                                String[] line = text.split(" ");
                                                length = line.length;
                                                String path = "file add " + args[2] + " " + args[3];
                                                for (int i = 4 ;i < args.length - 2; i++) {
                                                    path = path + " " + args[i];
                                                }
                                                path = path + " " + "new " + length;
                                                out.println(path);
                                                out.flush();
                                                for (int i = 0; i < line.length; i++) {
                                                    out.println(line[i]);
                                                    out.flush();
                                                }
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                                    }
                                }
                            }
                        }
                        else if (args[1].equals("remove") && args.length >= 5) {
                            if (filetypes.contains(args[2])) {
                                try {
                                    comp = Integer.parseInt(args[args.length - 1]);
                                    if (!outputs.containsKey(comp)) {
                                        updateDisplay("\"" + args[4] + "\" is not a valid client id", 1);
                                    }
                                    else {
                                        out = outputs.get(comp);
                                        String command = "file remove " + args[2] + " " + args[3];
                                        for (int i = 4; i < args.length - 1; i ++) {
                                            command = command + " " + args[i];
                                        }
                                        out.println(command);
                                        out.flush();
                                    }
                                }
                                catch (NumberFormatException e) {
                                    updateDisplay("\"" + args[4] + "\" is not a valid number", 1);
                                }
                            }
                            else updateDisplay("\"" + args[2] + "\" is not a valid filetype", 1);
                        }
                        else if (args[1].equals("mkdir") && args.length >= 4) {
                            try {
                                comp = Integer.parseInt(args[args.length- 1]);
                                if (!outputs.containsKey(comp)) updateDisplay("\"" + args[3] + "\" is not a valid client id", 1);
                                else {
                                    out = outputs.get(comp);
                                    String command = "file mkdir " +args[2];
                                    for (int i = 3; i < args.length - 1; i++) {
                                        command = command + " " + args[i];
                                    }
                                    out.println(command);
                                    out.flush();
                                }
                            } catch (NumberFormatException e) {
                                updateDisplay("\"" + args[3] + "\" is not a valid number", 1);
                            }
                        }
                        else if (args[1].equals("rmdir") && args.length >= 4) {
                            try {
                                comp = Integer.parseInt(args[args.length- 1]);
                                if (!outputs.containsKey(comp)) updateDisplay("\"" + args[3] + "\" is not a valid client id", 1);
                                else {
                                    out = outputs.get(comp);
                                    String command = "file rmdir " +args[2];
                                    for (int i = 3; i < args.length - 1; i++) {
                                        command = command + " " + args[i];
                                    }
                                    out.println(command);
                                    out.flush();
                                }
                            } catch (NumberFormatException e) {
                                updateDisplay("\"" + args[3] + "\" is not a valid number", 1);
                            }
                        }
                        else {
                            updateDisplay("\"" + args[1] + "\" is not a valid argument", 1);
                        }
                    }
                    else if ((args[0].equals("ls") || args[0].equals("pwd") || args[0].equals("cd")) && args.length >= 2) {
                        try {
                            comp = Integer.parseInt(args[1]);
                            if (!outputs.containsKey(comp)) {
                                updateDisplay("\"" + args[1] + "\" is not a valid client id", 1);
                            }
                            else {
                                out = outputs.get(comp);
                                if (args[0].equals("ls") || args[0].equals("pwd")) {
                                    out.println(args[0]);
                                    out.flush();
                                }
                                else {
                                    String command = "cd";
                                    for (int i = 2; i < args.length; i++) {
                                        command = command + " " + args[i];
                                    }
                                    out.println(command);
                                    out.flush();
                                }
                            }
                        }
                        catch (NumberFormatException e) {
                            updateDisplay("\"" + args[1] + "\" is not a valid number", 1);
                        }
                    }
                    else if (args[0].equals("sound") && args.length >= 4) {
                        int length = 0;
                        String path = args[2];
                        for (int i = 3; i < args.length - 1; i++) {
                            path = path + " " + args[i];
                        }
                        if (args[1].equals("add")) {
                            File mp3 = new File("files/" + path + ".mp3");
                            if (!mp3.exists()) {
                                updateDisplay("\"" + path + "\" cannot be found", 1);
                            }
                            else {
                                try {
                                    comp = Integer.parseInt(args[args.length - 1]);
                                    if (!sockets.containsKey(comp)) updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                                    else {
                                        out = outputs.get(comp);
                                        OutputStream outputStream = sockets.get(comp).getOutputStream();
                                        InputStream inputStream = new FileInputStream(mp3);
                                        int count;
                                        byte[] bytes = new byte[32*1024];
                                        while ((count = inputStream.read(bytes)) > 0) length = length + count;
                                        System.out.println(length);
                                        inputStream = new FileInputStream(mp3);
                                        out.println("sound add " + path + " " + length);
                                        out.flush();
                                        while ((count = inputStream.read(bytes)) > 0) outputStream.write(bytes, 0, count);
                                        inputStream.close();
                                        updateDisplay("Uploaded sound", 1);
                                    }
                                }
                                catch (NumberFormatException e) {
                                    updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                                }
                            }
                        }
                        else if (args[1].equals("remove")) {
                            try {
                                comp = Integer.parseInt(args[args.length-1]);
                                if (!outputs.containsKey(comp)) updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                                else {
                                    out = outputs.get(comp);
                                    out.println("sound remove " + args[2]);
                                    out.flush();
                                }
                            }
                            catch (NumberFormatException e) {
                                updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                            }
                        }
                        else updateDisplay("\"" + args[1] + "\" is not a valid argument", 1);
                    }
                    else if (args[0].equals("filetypes")) {
                        if (args.length == 1) {
                            String string = "";
                            for (int i = 0; i < filetypes.size(); i++) {
                                string = string + filetypes.get(i) + "\n";
                            }
                            updateDisplay(string, 1);
                        }
                        else if (args.length == 3) {
                            if (args[1].equals("add")) {
                                if (!filetypes.contains(args[2])) {
                                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File("filetypes.txt"), true));
                                    writer.write(args[2]);
                                    writer.newLine();
                                    writer.close();
                                    filetypes.add(args[2]);
                                    updateDisplay("Filetype successfully added", 1);
                                }
                                else {
                                    updateDisplay("Filetype already exists", 1);
                                }
                            }
                            else if (args[1].equals("remove")) {
                                if (filetypes.contains(args[2])) {
                                    List<String> temp = new ArrayList<>();
                                    String string;
                                    File types = new File("filetypes.txt");
                                    Scanner scanner = new Scanner(new FileInputStream(types));
                                    while (scanner.hasNext()) {
                                        string = scanner.nextLine();
                                        if (!string.equals(args[2])) temp.add(string);
                                    }
                                    PrintWriter writer = new PrintWriter(new FileWriter(types));
                                    for (int i = 0; i < temp.size(); i++) {
                                        writer.println(temp.get(i));
                                        writer.flush();
                                    }
                                    writer.close();
                                    filetypes.remove(args[2]);
                                    updateDisplay("Filetype successfully removed", 1);
                                }
                                else {
                                    updateDisplay("Filetype does not exist", 1);
                                }
                            }
                        }
                    }
                    else if (args[0].equals("run") && args.length >= 3) {
                        try {
                            comp = Integer.parseInt(args[args.length - 1]);
                            if (!outputs.containsKey(comp)) updateDisplay("\"" + args[args.length - 1] + "\" is not a valid client id", 1);
                            else {
                                out = outputs.get(comp);
                                out.print("run");
                                for (int i = 1; i < args.length - 1; i ++) {
                                    out.print(" " + args[i]);
                                }
                                out.println();
                                out.flush();
                            }
                        }
                        catch (NumberFormatException e) {
                            updateDisplay("\"" + args[args.length - 1] + "\" is not a valid number", 1);
                        }
                    }
                    else if (args[0].equals("files")) {
                        File file = new File("files/");
                        System.out.println(file.getAbsolutePath());
                        String[] list = file.list();
                        String display = "";
                        for (int i = 0; i < list.length; i++) {
                            display = display + list[i] + "\n";
                        }
                        updateDisplay(display,1);
                    }
                    else {
                        updateDisplay("Not a valid command", 1);
                    }
                }
            } catch (IOException e) {
                updateDisplay("Output stream lost", 1);
            }
        }
    }
    //true == add, false == remove
    private synchronized void modifyConnections(boolean addRem, int id, Socket socket, PrintWriter out, BufferedReader in, String name, List<String> soundList) {
        if (addRem) {
            ids.add(id);
            sockets.put(id, socket);
            outputs.put(id, out);
            ins.put(id, in);
            names.put(id, name);
            sounds.put(id, soundList);
        }
        else {
            Integer i = id;
            sockets.remove(id);
            outputs.remove(id);
            ins.remove(id);
            names.remove(id);
            sounds.remove(id);
            ids.remove(i);
        }
    }

    private synchronized String updateDisplay(String msg, int display) {
        System.out.print("\033[H\033[2J");
        if (display == 1) {
            displayMsg = msg;
            System.out.println("Prank Server ©\nCommands\nfile <add/remove> <file type> <name> <source/new> <client id>\nfile <mkdir/rmdir> <name> <client id>, files (files to upload)");
            System.out.println("play <sound> <(optional) duration> <client id>\nlistsounds <client id>\nls <client id>\npwd <client id>");
            System.out.println("listsounds <client id>\nsounds <add/remove <name> <client id");
            System.out.println("filetypes, filetypes <add/remove> <name>");
            System.out.println("run <relative path or full path>");
            System.out.println();
            for (int i = 0; i < ids.size(); i++) {
                System.out.printf("[%d] Connected with -- %s\n", ids.get(i), names.get(ids.get(i)));
            }
            System.out.println();
            if (displayMsg != null) {
                System.out.println(displayMsg + "\n");
            }
            return null;
        }
        else if (display == 2) {
            System.out.println("Please enter the text of the file:");
            try {
                return (new BufferedReader(new InputStreamReader(System.in))).readLine();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public class Connection implements Runnable {
        private BufferedReader in;
        private int id;

        public Connection(int id) {
            this.id = id;
            in = ins.get(id);
        }

        @Override
        public void run() {
            Clock clock = new Clock();
            Thread pings = new Thread(new MessageHandler(in, clock, id));
            pings.start();
            while (true) {
                try {
                    if (clock.changeTime(false) == 0) {
                        Thread.sleep(3000);
                        pings.interrupt();
                        modifyConnections(false, id, null, null, null, null, null);
                        updateDisplay(null, 1);
                        return;
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private static class Clock{
        int clock;
        public Clock() {
            clock = 15;
        }
        private synchronized int changeTime(boolean upDown) {
            if (upDown) clock = 15;
            else clock--;

            return clock;
        }
    }

    private class MessageHandler implements Runnable {

        private BufferedReader in;
        Clock clock;
        int id;
        public MessageHandler(BufferedReader in, Clock clock, int id) {
            this.in = in;
            this.clock = clock;
            this.id = id;
        }
        @Override
        public void run() {
            String input;
            String[] args;
            while (true) {
                try {
                    input = in.readLine();
                    args = input.split(" ");
                    if (input != null) {
                        if (input.equals("ping")) clock.changeTime(true);
                        else if (args[0].equals("filepath"))  {
                            clock.changeTime(true);
                            input = input.substring(input.indexOf("C:\\"));
                            updateDisplay(input, 1);
                        }
                        else if (args[0].equals("filelist")) {
                            clock.changeTime(true);
                            input = "";
                            for (int i = 1; i < args.length; i++) {
                                args[i] = args[i].replaceAll("\\\\32", " ");
                                input = input + args[i] + "\n";
                            }
                            updateDisplay(input, 1);
                        }
                        else if (args[0].equals("sound")) {
                            clock.changeTime(true);
                            if (args[1].equals("good")) {
                                if (args[2].equals("add")) {
                                    List<String> temp = sounds.get(id);
                                    String name = args[3];
                                    for (int i = 4; i < args.length; i++) {
                                        name = name + " " + args[i];
                                    }
                                    temp.add(name);
                                    sounds.replace(id, temp);
                                    updateDisplay("Sound successfully added", 1);
                                }
                                else if (args[2].equals("remove")) {
                                    List<String> temp = sounds.get(id);
                                    String name = args[3];
                                    for (int i = 4; i < args.length; i++) {
                                        name = name + " " + args[i];
                                    }
                                    temp.remove(name);
                                    sounds.replace(id, temp);
                                    updateDisplay("Sound successfully removed", 1);
                                }
                            }
                            else if (args[1].equals("err")) {
                                if (args[2].equals("corrupt")) updateDisplay("The sound file was corrupted on transfer", 1);
                                else if (args[2].equals("lost_connection")) updateDisplay("Connection was lost during sound upload", 1);
                                else if (args[2].equals("fail_create")) updateDisplay("The sound file could not be created", 1);
                                else if (args[2].equals("not_exist")) updateDisplay("The sound file does not exist", 1);
                            }
                        }
                        else if (args[0].equals("file")) {
                            if (args[1].equals("good")) {
                                clock.changeTime(true);
                                if (args[2].equals("add")) {
                                    updateDisplay("File successfully added", 1);
                                }
                                else if (args[2].equals("run")) {
                                    updateDisplay("File successfully started", 1);
                                }
                                else {
                                    updateDisplay("File successfully removed", 1);
                                }
                            }
                            else if (args[1].equals("err")) {
                                clock.changeTime(true);
                                if (args[2].equals("cant_make")) {
                                    updateDisplay("Error creating file",1);
                                }
                                else if (args[2].equals("exists")) {
                                    updateDisplay("File already exists",1);
                                }
                                else if (args[2].equals("not_exists")) {
                                    updateDisplay("The file does not exist",1);
                                }
                                else if (args[2].equals("cant_delete")) {
                                    updateDisplay("Error deleting file",1);
                                }
                            }
                            else if (args[1].equals("uploading")) {
                                clock.changeTime(true);
                            }
                        }
                        else if (args[0].equals("cd")) {
                            outputs.get(id).println("pwd");
                            outputs.get(id).flush();
                        }
                    }
                    else {
                        Thread.sleep(1000);
                    }
                    Thread.sleep(200);
                } catch (IOException | InterruptedException | NullPointerException e) {
                    return;
                }
            }
        }
    }

    public class GetConnections implements Runnable{
        @Override
        public void run() {
            try {
                ServerSocket server = new ServerSocket(8888);
                while (true) {
                    String name = "", temp;
                    String[] args;
                    List<String> sounds = new ArrayList<>();
                    Socket socket = server.accept();
                    int i = 1;
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out =  new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    for (int j = 0; j < 2; j++) {
                        temp = in.readLine();
                        args = temp.split(" ");
                        if (args[0].equals("name")) name = args[1];
                        else if (args[0].equals("sounds")) {
                            for (int k = 1; k < args.length; k++) {
                                sounds.add(args[k]);
                            }
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    while (ids.contains(i)) {
                        i++;
                    }
                    modifyConnections(true, i, socket, out, in, name, sounds);
                    System.out.println("\033[H\033[2J");
                    updateDisplay(null, 1);
                    socket.setSoTimeout(10000);
                    Thread lost = new Thread(new Connection(i));
                    lost.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.print("\033[H\033[2J");
        PrankServer server = new PrankServer();
        server.serverStart();
    }
}