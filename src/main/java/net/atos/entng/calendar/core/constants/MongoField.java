package net.atos.entng.calendar.core.constants;

public class MongoField {

    private MongoField() {
        throw new IllegalStateException("Utility class");
    }

    public static final String $DATE = "$date";
    public static final String $OR = "$or";
    public static final String $AND = "$and";
    public static final String $GREATER_THAN = "$gt";
    public static final String $GREATER_OR_EQUAL = "$gte";
    public static final String $LESSER_THAN = "$lt";
    public static final String $LESSER_OR_EQUAL = "$lte";

}
