public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            BankSampahCLI.main(args);
            return;
        }
        if (args.length > 0 && "--web".equalsIgnoreCase(args[0])) {
            web.WebServer.launch(args);
            return;
        }
        BankSampahGUI.launch();
    }
}
