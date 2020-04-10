public class MessageSender implements Runnable {

    private final byte[] message;
    private final Channel channel;

    public MessageSender(byte[] message, Channel channel) {
        this.message = message;
        this.channel = channel;
    }

    @Override
    public void run() {
        this.channel.sendMessage(this.message);
    }
}
