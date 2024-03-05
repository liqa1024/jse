package io.github.spencerpark.jupyter.kernel.magic;

import java.util.List;

@SuppressWarnings("UnnecessaryModifier")
public interface LineMagicArgs {
    public static LineMagicArgs of(String name, List<String> args) {
        return new LineMagicArgs() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<String> getArgs() {
                return args;
            }
        };
    }

    public String getName();

    public List<String> getArgs();
}
