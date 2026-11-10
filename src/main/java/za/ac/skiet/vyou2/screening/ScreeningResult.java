package za.ac.skiet.vyou2.screening;

import java.util.List;

public record ScreeningResult(boolean passed, List<String> reasons) {

    public static ScreeningResult pass() {
        return new ScreeningResult(true, List.of());
    }

    public static ScreeningResult reject(String reason) {
        return new ScreeningResult(false, List.of(reason));
    }
}
