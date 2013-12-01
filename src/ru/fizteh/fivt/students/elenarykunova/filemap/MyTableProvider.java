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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.elenarykunova.shell.Shell;
import ru.fizteh.fivt.students.elenarykunova.shell.Shell.ExitCode;
import org.json.*;

public class MyTableProvider implements TableProvider, AutoCloseable {

    private String rootDir = null;
    private HashMap<String, MyTable> tables = new HashMap<String, MyTable>();
    private Lock write = new ReentrantLock(true);
    private volatile boolean isClosed = false;

    public MyTableProvider() {
    }

    public MyTableProvider(String newRootDir) {
        rootDir = newRootDir;
        tables = new HashMap<String, MyTable>();
        isClosed = false;
    }

    public String getPath(String tableName) {
        if (rootDir == null) {
            return null;
        }
        return rootDir + File.separator + tableName;
    }

    private boolean isEmpty(String str) {
        return (str == null || str.trim().isEmpty());
    }

    protected boolean hasBadSymbols(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' || c == '/' || c == '.' || c == ':' || c == '*' || c == '?' || c == '|' || c == '"'
                    || c == '<' || c == '>' || c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '('
                    || c == ')') {
                return true;
            }
        }
        if (str.split("[\\s]").length > 1) {
            return true;
        }
        return false;
    }

    private void checkClosed() throws IllegalStateException {
        if (isClosed) {
            throw new IllegalStateException("is closed");
        }
    }

    // read and write sometimes
    @Override
    public Table getTable(String name) throws IllegalArgumentException, RuntimeException {
        checkClosed();
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
        write.lock();
        try {
            File tmpFile = new File(tablePath);
            if (!tmpFile.exists() || !tmpFile.isDirectory()) {
                return null;
            }
            File info = new File(tablePath + File.separator + "signature.tsv");
            if (!info.exists() || info.length() == 0) {
                throw new RuntimeException(name + " exists as folder and has no data as table");
            }
            List<Class<?>> oldTypes = new ArrayList<Class<?>>();

            if (info.exists()) {
                try {
                    oldTypes = getTypesFromSignature(info);
                } catch (IOException e) {
                    throw new RuntimeException(name + " can't get info from signature", e);
                }
            }
            try {
                if (tables.get(name) != null) {
                    return (Table) tables.get(name);
                } else {
                    MyTable result = new MyTable(tablePath, name, this, oldTypes);
                    tables.put(name, result);
                    return (Table) result;
                }
            } catch (IOException e1) {
                throw new RuntimeException("can't read info from signature.tsv", e1);
            }
        } finally {
            write.unlock();
        }
    }

    private boolean isCorrectType(Class<?> type) {
        if (type == null) {
            return false;
        }
        return (type.equals(Integer.class) || type.equals(Long.class) || type.equals(Byte.class)
                || type.equals(Float.class) || type.equals(Double.class) || type.equals(Boolean.class) || type
                    .equals(String.class));
    }

    private void writeTypes(File info, List<Class<?>> types) throws IOException {
        Throwable e = null;
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(info);
            for (int i = 0; i < types.size(); i++) {
                Class<?> type = types.get(i);
                if (type.equals(Integer.class)) {
                    os.write("int".getBytes());
                } else if (type.equals(String.class)) {
                    os.write("String".getBytes());
                } else if (type.equals(Boolean.class)) {
                    os.write("boolean".getBytes());
                } else if (type.equals(Float.class)) {
                    os.write("float".getBytes());
                } else if (type.equals(Double.class)) {
                    os.write("double".getBytes());
                } else if (type.equals(Byte.class)) {
                    os.write("byte".getBytes());
                } else if (type.equals(Long.class)) {
                    os.write("long".getBytes());
                } else {
                    throw new IOException("unexpected type in table: " + type.getSimpleName());
                }
                if (i != types.size() - 1) {
                    os.write(" ".getBytes());
                }
            }
        } catch (IOException t) {
            e = t;
            throw t;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable e1) {
                    if (e != null) {
                        e.addSuppressed(e1);
                    }
                }
            }
        }
    }

    protected Class<?> getTypeFromString(String type) throws IOException {
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

    private List<Class<?>> getTypesFromSignature(File info) throws IOException {
        Throwable e = null;
        List<Class<?>> types = new ArrayList<Class<?>>();
        FileInputStream is = null;
        try {
            is = new FileInputStream(info);
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
        } catch (IOException t) {
            e = t;
            throw t;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e1) {
                    if (e != null) {
                        e.addSuppressed(e1);
                    }
                }
            }
        }
        return types;
    }

    // write
    @Override
    public Table createTable(String name, List<Class<?>> columnTypes) throws IllegalArgumentException,
            RuntimeException, IOException {
        checkClosed();
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

        write.lock();
        try {
            File tmpFile = new File(tablePath);

            File info = new File(tablePath + File.separator + "signature.tsv");
            List<Class<?>> oldTypes = new ArrayList<Class<?>>();
            if (info.exists()) {
                oldTypes = getTypesFromSignature(info);
            }

            if (tmpFile.exists() && tmpFile.isDirectory()) {
                if (!info.exists()) {
                    throw new IllegalArgumentException(name + " exists, but couldn't find table info");
                } else {
                    if (oldTypes.size() != columnTypes.size()) {
                        throw new IllegalArgumentException(name + " exists, but number of types mismatch");
                    }
                    for (int i = 0; i < oldTypes.size(); i++) {
                        if (!oldTypes.get(i).equals(columnTypes.get(i))) {
                            throw new IllegalArgumentException(name + " exists, but types mismatch");
                        }
                    }
                    if (tables.get(name) == null) {
                        MyTable result = new MyTable(tablePath, name, this, columnTypes);
                        tables.put(name, result);
                    }
                }
                return null;
            } else {
                if (!tmpFile.mkdir() || !info.createNewFile()) {
                    throw new RuntimeException(name + " can't create a table");
                } else {
                    writeTypes(info, columnTypes);
                    if (tables.get(name) == null) {
                        MyTable result = new MyTable(tablePath, name, this, columnTypes);
                        tables.put(name, result);
                        return (Table) result;
                    } else {
                        return null;
                    }
                }
            }
        } finally {
            write.unlock();
        }
    }

    // write
    public void removeTable(String name) throws RuntimeException, IllegalArgumentException, IllegalStateException {
        checkClosed();
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
        write.lock();
        try {
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
        } finally {
            write.unlock();
        }
    }

    @Override
    public Storeable deserialize(Table table, String value) throws ParseException, IllegalArgumentException {
        checkClosed();
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
            throw new ParseException("deserialize: number of elements mismatch", 0);
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
            throw new ParseException("deserialize: can't create new storeable " + e.getMessage(), 0);
        } catch (IndexOutOfBoundsException e2) {
            throw new ParseException("deserialize: can't create new storeable " + e2.getMessage(), 0);
        }
    }

    @Override
    public String serialize(Table table, Storeable value) throws ColumnFormatException {
        checkClosed();
        if (value == null) {
            throw new RuntimeException("no value to serialize found");
        }
        Object[] array = new Object[table.getColumnsCount()];
        for (int i = 0; i < table.getColumnsCount(); i++) {
            if (value.getColumnAt(i) != null && !table.getColumnType(i).equals(value.getColumnAt(i).getClass())) {
                throw new ColumnFormatException(value.getColumnAt(i).getClass() + " serialize: types mismatch");
            }
            array[i] = value.getColumnAt(i);
        }
        try {
            JSONArray json = new JSONArray(array);
            return json.toString();
        } catch (JSONException e) {
            throw new ColumnFormatException("can't make string from this Storeable");
        }
    }

    @Override
    public Storeable createFor(Table table) {
        checkClosed();
        return (Storeable) new MyStoreable(table);
    }

    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException,
            IndexOutOfBoundsException {
        checkClosed();
        return (Storeable) new MyStoreable(table, values);
    }

    @Override
    public String toString() {
        String className = MyTableProvider.class.getSimpleName();
        return (className + "[" + rootDir + "]");
    }

    protected void removeTableFromMap(String key) {
        write.lock();
        try {
            if (!isClosed) {
                tables.remove(key);
            }
        } finally {
            write.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        write.lock();
        try {
            isClosed = true;
            for (MyTable table : tables.values()) {
                if (table != null) {
                    table.close();
                }
            }
            tables.clear();
        } finally {
            write.unlock();
        }
    }
}
