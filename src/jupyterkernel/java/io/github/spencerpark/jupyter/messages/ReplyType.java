package io.github.spencerpark.jupyter.messages;

@SuppressWarnings("UnnecessaryModifier")
public interface ReplyType<Req> {
    public MessageType<Req> getRequestType();
}
