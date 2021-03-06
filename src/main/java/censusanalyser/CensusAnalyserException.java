package censusanalyser;

public class CensusAnalyserException extends Exception {

    enum ExceptionType {
        UNABLE_TO_PARSE, PROBLEM_IN_FIELDS, NO_CENSUS_DATA, CSV_FILE_PROBLEM, NO_STATE_CODE_DATA, EMPTY_FIELDS;
    }

    ExceptionType type;

    public CensusAnalyserException(String message, String name) {
        super(message);
        this.type = ExceptionType.valueOf(name);
    }

    public CensusAnalyserException(String message, ExceptionType type) {
        super(message);
        this.type = type;
    }

    public CensusAnalyserException(String message, ExceptionType type, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
}
