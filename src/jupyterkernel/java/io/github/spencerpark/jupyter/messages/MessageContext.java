package io.github.spencerpark.jupyter.messages;

import java.util.List;

@SuppressWarnings({"UnnecessaryModifier", "rawtypes"})
public interface MessageContext {
    public List<byte[]> getIdentities();

    public Header getHeader();
}
