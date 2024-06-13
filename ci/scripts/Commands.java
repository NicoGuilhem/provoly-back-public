import java.util.Arrays;

public enum Commands {
    SELECT_TARGET_MODULES("select-target-modules"),
    UPDATE_DEPLOYMENT("select-deploy-modules");

    private String value;

    Commands(String value) {
        this.value = value;
    }

    public static Commands from(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nope"));
    }
}
