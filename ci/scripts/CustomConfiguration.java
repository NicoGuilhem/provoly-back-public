
//DEPS io.quarkus:quarkus-picocli

import picocli.CommandLine;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;

@Dependent
class CustomConfiguration {

    @Produces
    CommandLine getCommandLine(PicocliCommandLineFactory factory) {
        return factory.create().registerConverter(Commands.class, s -> Commands.from(s));
    }
}
