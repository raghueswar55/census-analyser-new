package censusanalyser;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;

public class CensusDAOBuilder {
    Map<String, CensusDAO> censusDataMap = new HashMap<>();

    private enum FieldClassesList {
        stateCode(IndiaStateCodeCSV.class);
        private final Class _class;
        FieldClassesList(Class _class) {
            this._class = _class;
        }
    }

    private enum CountryCsvClasses {
        INDIA(IndiaCensusCSV.class), US(USCensusCSV.class);
        private final Class<? extends CensusCSV> _class;
        CountryCsvClasses(Class<? extends CensusCSV> _class) {
            this._class = _class;
        }
    }

    public Map<String, CensusDAO> loadCensusData(String country, String... censusCsvFilePath) throws CensusAnalyserException, IllegalAccessException {
        Class<? extends CensusCSV> countryClass = CountryCsvClasses.valueOf(country)._class;
        return loadCensusData(countryClass,censusCsvFilePath);
    }

    private <T extends CensusCSV> Map<String, CensusDAO> loadCensusData(Class<T> csvClass, String... censusCsvFilePath) throws CensusAnalyserException, IllegalAccessException {
        try (Reader reader = Files.newBufferedReader(Paths.get(censusCsvFilePath[0]))) {
            ICSVBuilder csvBuilder = CSVBuilderFactory.getCsvBuilder();
            Iterator<T> csvIterator = csvBuilder.getCsvIterator(reader, csvClass);
            Iterable<T> csvIterable = () -> csvIterator;
            StreamSupport.stream(csvIterable.spliterator(), false)
                         .parallel()
                         .forEach(t -> censusDataMap.put(t.getStateName(), new CensusDAO(t)));
        } catch (IOException e) {
            throw new CensusAnalyserException(e.getMessage(),
                    CensusAnalyserException.ExceptionType.CSV_FILE_PROBLEM);
        } catch (CSVBuilderException e) {
            throw new CensusAnalyserException(e.getMessage(), e.type.name());
        }
        loadEmptyFields(censusCsvFilePath);
        return censusDataMap;
    }

    private void loadEmptyFields(String[] censusCsvFilePath) throws CensusAnalyserException, IllegalAccessException {
        String emptyField = getEmptyFields();
        String newEmptyField = emptyField;
        if (emptyField != null) {
            for (String path : censusCsvFilePath) {
                try {
                    loadEmptyFieldData(emptyField, path);
                    newEmptyField = getEmptyFields();
                    if (!emptyField.equals(newEmptyField)) {
                        break;
                    }
                } catch (CensusAnalyserException e){
                    if (!(e.type.toString().equals("PROBLEM_IN_FIELDS")))
                        throw e;
                }
            }
            if (emptyField.equals(newEmptyField) )
                throw new CensusAnalyserException("given files having empty fields", CensusAnalyserException.ExceptionType.EMPTY_FIELDS);
            loadEmptyFields(censusCsvFilePath);
        }
    }

    private String getEmptyFields() throws IllegalAccessException {
        for (CensusDAO censusDAO: censusDataMap.values()) {
            Field[] fields = censusDAO.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.get(censusDAO) == null) {
                    return field.getName();
                }
            }
        }
        return null;
    }

    private void loadEmptyFieldData(String fieldName, String path) throws CensusAnalyserException {
        Class fieldClass = FieldClassesList.valueOf(fieldName)._class;
        loadEmptyFieldData(path, fieldClass,fieldName);
    }

    private  <T> void loadEmptyFieldData(String csvFilePath, Class<T> _class, String field) throws CensusAnalyserException {
        try (Reader reader = Files.newBufferedReader(Paths.get(csvFilePath))) {
            ICSVBuilder csvBuilder = CSVBuilderFactory.getCsvBuilder();
            Iterator<T> csvIterator = csvBuilder.getCsvIterator(reader, _class);
            Iterable<T> csvIterable = () -> csvIterator;
            StreamSupport.stream(csvIterable.spliterator(), false).parallel().forEach((t) -> {
                try {
                    String stateName = (String) getProperty(t, "stateName");
                    CensusDAO censusDAO = censusDataMap.get(stateName);
                    if (censusDAO != null)  {
                        Object value = getProperty(t, field);
                        setProperty(censusDAO, field, value);
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            throw new CensusAnalyserException(e.getMessage(),
                    CensusAnalyserException.ExceptionType.CSV_FILE_PROBLEM);
        } catch (CSVBuilderException e) {
            throw new CensusAnalyserException(e.getMessage(), e.type.name());
        }
    }

}











