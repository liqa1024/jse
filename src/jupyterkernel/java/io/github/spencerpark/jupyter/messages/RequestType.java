package io.github.spencerpark.jupyter.messages;

@SuppressWarnings("UnnecessaryModifier")
public interface RequestType<Rep> {
    public MessageType<Rep> getReplyType();
}
