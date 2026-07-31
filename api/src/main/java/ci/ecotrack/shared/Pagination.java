package ci.ecotrack.shared;

public record Pagination(int page, int size) {

    public static final int PAGE_MIN = 0;
    public static final int SIZE_MIN = 1;
    public static final int SIZE_MAX = 100;

    public Pagination {
        if (page < PAGE_MIN) {
            throw new DonneeInvalideException(
                    "Le parametre page doit etre >= " + PAGE_MIN);
        }
        if (size < SIZE_MIN || size > SIZE_MAX) {
            throw new DonneeInvalideException(
                    "Le parametre size doit etre compris entre "
                            + SIZE_MIN + " et " + SIZE_MAX);
        }
    }
}
