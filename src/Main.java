public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            BankSampahCLI.main(args);
            return;
        }
        BankSampahGUI.launch();
    }
}
