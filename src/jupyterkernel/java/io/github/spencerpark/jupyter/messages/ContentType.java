package io.github.spencerpark.jupyter.messages;

@SuppressWarnings("UnnecessaryModifier")
public interface ContentType<T> {
    public MessageType<T> getType();
}
