package ru.fizteh.fivt.students.elenarykunova.filemap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.elenarykunova.shell.Shell;
import ru.fizteh.fivt.students.elenarykunova.shell.Shell.ExitCode;
import org.json.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MyTableProvider implements TableProvider {

    private String rootDir = null;
    private HashMap<String, Filemap> tables = new HashMap<String, Filemap>();
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private Lock read = readWriteLock.readLock();
    private Lock write = readWriteLock.writeLock();
    private ThreadLocal<String> currentTable = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return null;
        }
    };

    public MyTableProvider() {
    }

    public MyTableProvider(String newRootDir) {
        rootDir = newRootDir;
        tables = new HashMap<String, Filemap>();
    }

    public String getPath(String tableName) {
        if (rootDir == null) {
            return null;
        }
        return rootDir + File.separator + tableName;
    }

    public boolean isEmpty(String str) {
        return (str == null || str.isEmpty() || str.trim().isEmpty());
    }

    public boolean hasBadSymbols(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' || c == '/' || c == '.' || c == ':' || c == '*'
                    || c == '?' || c == '|' || c == '"' || c == '<' || c == '>'
                    || c == ' ' || c == '\t' || c == '\n' || c == '\r'
                    || c == '(' || c == ')') {
                return true;
            }
        }
        if (str.split("[\\s]").length > 1) {
            return true;
        }
        return false;
    }

    @Override
    public Table getTable(String name) throws IllegalArgumentException,
            RuntimeException {
        if (isEmpty(name)) {
            throw new IllegalArgumentException("name of table is empty");
        }
        if (hasBadSymbols(name)) {
            throw new RuntimeException("name of table contains bad symbol");
        }
        String tablePath = getPath(name);
        if (tablePath == null) {
            throw new RuntimeException("no root directory");
        }
        File tmpFile = new File(tablePath);
        if (!tmpFile.exists() || !tmpFile.isDirectory()) {
            return null;
        }
        File info = new File(tablePath + File.separator + "signature.tsv");
        if (!info.exists() || info.length() == 0) {
            throw new RuntimeException(name
                    + " exists as folder and has no data as table");
        }
        List<Class<?>> oldTypes = new ArrayList<Class<?>>();
        if (info.exists()) {
            try {
                oldTypes = getTypesFromSignature(info);
            } catch (IOException e) {
                throw new RuntimeException(name
                        + " can't get info from signature", e);
            }
        }
        try {
            if (tables.get(name) != null) {
                return (Table) tables.get(name);
            } else {
                try {
                    write.lock();
                    Filemap result = new Filemap(tablePath, name, this, oldTypes);
                    tables.put(name, result);
                    return (Table) result;
                } finally {
                    write.unlock();
                }
            }
        } catch (IOException e1) {
            throw new RuntimeException("can't read info from signature.tsv", e1);
        }
    }

    public boolean isCorrectType(Class<?> type) {
        if (type == null) {
            return false;
        }
        return (type.equals(Integer.class) || type.equals(Long.class)
                || type.equals(Byte.class) || type.equals(Float.class)
                || type.equals(Double.class) || type.equals(Boolean.class) || type
                    .equals(String.class));
    }

    public void writeTypes(File info, List<Class<?>> types) throws IOException {
        FileOutputStream os;
        os = new FileOutputStream(info);

        for (Class<?> type : types) {
            switch (type.getSimpleName()) {
            case "Integer":
                os.write("int".getBytes());
                break;
            case "Long":
                os.write("long".getBytes());
                break;
            case "Double":
                os.write("double".getBytes());
                break;
            case "Byte":
                os.write("byte".getBytes());
                break;
            case "Float":
                os.write("float".getBytes());
                break;
            case "Boolean":
                os.write("boolean".getBytes());
                break;
            case "String":
                os.write("String".getBytes());
                break;
            default:
                throw new IOException("unexpected type in table");
            }
            os.write(" ".getBytes());
        }
        os.close();
    }

    public Class<?> getTypeFromString(String type) throws IOException {
        switch (type) {
        case "int":
            return Integer.class;
        case "long":
            return Long.class;
        case "double":
            return Double.class;
        case "byte":
            return Byte.class;
        case "float":
            return Float.class;
        case "boolean":
            return Boolean.class;
        case "String":
            return String.class;
        default:
            throw new IOException(type + " types in signature.tsv mismatch");
        }

    }

    public List<Class<?>> getTypesFromSignature(File info) throws IOException {
        List<Class<?>> types = new ArrayList<Class<?>>();
        FileInputStream is;
        is = new FileInputStream(info);
        try {
            Scanner sc = new Scanner(is);
            sc.useDelimiter(" ");
            try {
                while (sc.hasNext()) {
                    String type = sc.next();
                    types.add(getTypeFromString(type));
                }
            } finally {
                sc.close();
            }
        } finally {
            is.close();
        }
        return types;
    }

    @Override
    public Table createTable(String name, List<Class<?>> columnTypes)
            throws IllegalArgumentException, RuntimeException, IOException {
        if (isEmpty(name)) {
            throw new IllegalArgumentException("name of table is empty");
        }
        if (hasBadSymbols(name)) {
            throw new RuntimeException("name of table contains bad symbol");
        }
        String tablePath = getPath(name);
        if (tablePath == null) {
            throw new RuntimeException("no root directory");
        }
        if (columnTypes == null || columnTypes.isEmpty()) {
            throw new IllegalArgumentException("list of types is empty");
        }
        for (Class<?> type : columnTypes) {
            if (!isCorrectType(type)) {
                throw new IllegalArgumentException(type + " wrong type");
            }
        }
        File tmpFile = new File(tablePath);

        File info = new File(tablePath + File.separator + "signature.tsv");
        List<Class<?>> oldTypes = new ArrayList<Class<?>>();
        if (info.exists()) {
            oldTypes = getTypesFromSignature(info);
        }

        if (tmpFile.exists() && tmpFile.isDirectory()) {
            if (!info.exists()) {
                throw new IllegalArgumentException(name
                        + " exists, but couldn't find table info");
            } else {
                if (oldTypes.size() != columnTypes.size()) {
                    throw new IllegalArgumentException(name
                            + " exists, but number of types mismatch");
                }
                for (int i = 0; i < oldTypes.size(); i++) {
                    if (!oldTypes.get(i).equals(columnTypes.get(i))) {
                        throw new IllegalArgumentException(name
                                + " exists, but types mismatch");
                    }
                }
                try {
                    read.lock();
                    if (tables.get(name) == null) {
                        try {
                            write.lock();
                            Filemap result = new Filemap(tablePath, name, this,
                                    columnTypes);
                            tables.put(name, result);
                        } finally {
                            write.unlock();
                        }
                    }
                } finally {
                    read.unlock();
                }
            }
            return null;
        } else {
            if (!tmpFile.mkdir() || !info.createNewFile()) {
                throw new RuntimeException(name + " can't create a table");
            } else {
                writeTypes(info, columnTypes);
                if (tables.get(name) == null) {
                    Filemap result = new Filemap(tablePath, name, this,
                            columnTypes);
                    tables.put(name, result);
                    return (Table) result;
                } else {
                    return null;
                }
            }

        }
    }

    public void removeTable(String name) throws RuntimeException,
            IllegalArgumentException, IllegalStateException {
        if (isEmpty(name)) {
            throw new IllegalArgumentException("name of table is empty");
        }
        if (hasBadSymbols(name)) {
            throw new RuntimeException("name of table contains bad symbol");
        }
        String tablePath = getPath(name);
        if (tablePath == null) {
            throw new RuntimeException("no root directory");
        }
        File tmpFile = new File(tablePath);
        if (!tmpFile.exists() || !tmpFile.isDirectory()) {
            throw new IllegalStateException(name + " not exists");
        } else {
            if (tables.get(name) != null) {
                tables.remove(name);
            }
            Shell sh = new Shell(rootDir, false);
            if (sh.rm(name) == ExitCode.OK) {
                return;
            } else {
                throw new RuntimeException(name + " can't remove table");
            }
        }
    }

    @Override
    public Storeable deserialize(Table table, String value)
            throws ParseException, IllegalArgumentException {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("deserialize: value is empty");
        }
        JSONArray json;
        try {
            json = new JSONArray(value);
        } catch (JSONException e) {
            throw new ParseException("deserialize: can't parse", 0);
        }
        if (json == null || json.length() != table.getColumnsCount()) {
            throw new ParseException(
                    "deserialize: number of elements mismatch", 0);
        }
        ArrayList<Object> values = new ArrayList<Object>(json.length());
        for (int i = 0; i < json.length(); i++) {
            if (json.get(i).equals(JSONObject.NULL)) {
                values.add(i, null);
            } else {
                values.add(i, json.get(i));
            }
        }
        try {
            return createFor(table, values);
        } catch (ColumnFormatException e) {
            throw new ParseException("deserialize: can't create new storeable "
                    + e.getMessage(), 0);
        } catch (IndexOutOfBoundsException e2) {
            throw new ParseException("deserialize: can't create new storeable "
                    + e2.getMessage(), 0);
        }
    }

    @Override
    public String serialize(Table table, Storeable value)
            throws ColumnFormatException {
        if (value == null) {
            throw new RuntimeException("no value to serialize found");
        }
        Object[] array = new Object[table.getColumnsCount()];
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (value.getColumnAt(i) != null
                    && !table.getColumnType(i).equals(
                            value.getColumnAt(i).getClass())) {
                throw new ColumnFormatException(value.getColumnAt(i).getClass()
                        + " serialize: types mismatch");
            }
            array[i] = value.getColumnAt(i);
        }
        try {
            JSONArray json = new JSONArray(array);
            return json.toString();
        } catch (JSONException e) {
            throw new ColumnFormatException(
                    "can't make string from this Storeable");
        }
    }

    @Override
    public Storeable createFor(Table table) {
        return (Storeable) new MyStoreable(table);
    }

    @Override
    public Storeable createFor(Table table, List<?> values)
            throws ColumnFormatException, IndexOutOfBoundsException {
        return (Storeable) new MyStoreable(table, values);
    }
}
