package ru.fizteh.fivt.students.olgagorbacheva.multyfilehashmap;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ru.fizteh.fivt.storage.strings.Table;
import ru.fizteh.fivt.students.olgagorbacheva.filemap.FileMapException;
import ru.fizteh.fivt.students.olgagorbacheva.filemap.Storage;

public class DataTable implements Table {

      private String dataBaseName;
      private Storage dataStorage;
      private Storage newKeys;
      private Set<String> removedKeys;
      private File dataBaseDir;

      public DataTable(String name, File directory) {
            dataBaseName = name;
            dataBaseDir = directory;
            dataStorage = new Storage();
            newKeys = new Storage();
            removedKeys = new TreeSet<String>();
      }

      public File getWorkingDirectory() {
            return dataBaseDir;
      }

      public String getName() {
            return dataBaseName;
      }

      public String get(String key) throws IllegalArgumentException {
            if (key == null) {
                  throw new IllegalArgumentException("Неверное значение ключа");
            }
            String value = newKeys.get(key);
            if (value != null) {
                  return value;
            } else {
                  value = dataStorage.get(key);
                  if (removedKeys.contains(key)) {
                        return null;
                  } else {
                        return value;
                  }
            }
      }

      public String put(String key, String value) {
            if (key == null || value == null || key.trim().equals("") || value.trim().equals("")) {
                  throw new IllegalArgumentException(
                              "Неверное значение ключа или значения");
            }
            if (dataStorage.get(key) == null && newKeys.get(key) == null
                        || removedKeys.contains(key)) {
                  if (removedKeys.contains(key)) {
                        removedKeys.remove(key);
                  } else {
                        newKeys.put(key, value);
                  }
                  return null;
            } else {
                  if (newKeys.get(key) == null) {
                        String oldValue = dataStorage.get(key);
                        if (!oldValue.equals(value)) {
                              newKeys.put(key, value);
                        }
                        return oldValue;
                  }
                  String oldValue = newKeys.get(key);
                  newKeys.set(key, value);
                  return oldValue;
            }
      }

      public String remove(String key) {
            if (key == null) {
                  throw new IllegalArgumentException("Неверное значение ключа");
            }
            String value = newKeys.get(key);
            if (value != null) {
                  newKeys.remove(key);
            } else {
                  value = dataStorage.get(key);
                  if (value != null) {
                        removedKeys.add(key);
                  }
            }
            return value;
      }

      public int size() {
            int size = dataStorage.getSize() - removedKeys.size();
            Set<String> keys = newKeys.keySet();
            for (String key : keys) {
                  if (dataStorage.get(key) == null) {
                        size++;
                  }
            }
            return size;
      }

      public int commit() {
            int size = 0;
            if (newKeys.getSize() != 0) {
                  Set<String> keys = newKeys.keySet();
                  for (String key : keys) {
                        size++;
                        if (!dataStorage.put(key, newKeys.get(key))) {
                              dataStorage.set(key, newKeys.get(key));
                        }
                  }
                  newKeys.clear();
            }
            if (!removedKeys.isEmpty()) {
                  for (String key : removedKeys) {
                        size++;
                        dataStorage.remove(key);
                  }
                  removedKeys.clear();
            }
            return size;
      }

      public int sizeChangesCommit() {
            return removedKeys.size() + newKeys.getSize();
      }

      public int rollback() {
            int size = 0;
            size += newKeys.getSize();
            newKeys.clear();
            size += removedKeys.size();
            removedKeys.clear();
            return size;
      }

      public void readFile() throws IOException {
            for (int i = 0; i < 16; i++) {
                  File currentDir = new File(dataBaseDir, String.valueOf(i)
                              + ".dir");
                  if (currentDir.exists() && currentDir.isDirectory()
                              && currentDir.canRead()) {
                        for (int j = 0; j < 16; j++) {
                              File currentFile = new File(currentDir,
                                          String.valueOf(j) + ".dat");
                              if (currentFile.exists() && currentFile.isFile()
                                          && currentDir.canRead()) {
                                    read(currentFile);
                              } else {
                                    if (currentFile.exists()
                                                && !(currentFile.isFile() && currentDir
                                                            .canRead())) {
                                          throw new IOException(
                                                      "База данных неподходящего формата");
                                    }
                              }
                        }
                  } else {
                        if (currentDir.exists()
                                    && !(currentDir.isDirectory() && currentDir
                                                .canRead())) {
                              throw new IOException(
                                          "База данных неподходящего формата");
                        }
                  }
            }
      }

      @SuppressWarnings("resource")
      public void read(File dataBaseFile) throws IOException {
            RandomAccessFile reader = new RandomAccessFile(dataBaseFile, "r");
            if (reader.length() == 0) {
                  return;
            }

            ArrayList<Integer> offsets = new ArrayList<Integer>();
            ArrayList<String> keys = new ArrayList<String>();
            ArrayList<String> values = new ArrayList<String>();

            try {
                  do {
                        ArrayList<Byte> keySymbols = new ArrayList<Byte>();
                        byte b = reader.readByte();
                        while (b != 0) {
                              keySymbols.add(b);
                              b = reader.readByte();
                        }
                        byte[] bytes = new byte[keySymbols.size()];
                        for (int i = 0; i < bytes.length; ++i) {
                              bytes[i] = keySymbols.get(i);
                        }
                        keys.add(new String(bytes, "UTF-8"));

                        int offset = reader.readInt();
                        if ((offset <= 0)
                                    || (!offsets.isEmpty() && offset <= offsets
                                                .get(offsets.size() - 1))
                                    || (offset >= reader.length())) {
                              throw new IOException("Неверное значение сдвигов");
                        }
                        offsets.add(offset);

                  } while (reader.getFilePointer() != offsets.get(0));
            } catch (EOFException e) {
                  throw new IOException("Файл законнчен раньше времени");
            }

            offsets.add((int) reader.length());

            for (int i = 0; i < keys.size(); ++i) {
                  byte[] bytes = new byte[offsets.get(i + 1) - offsets.get(i)];
                  reader.readFully(bytes);
                  values.add(new String(bytes, "UTF-8"));
            }

            for (int i = 0; i < keys.size(); ++i) {
                  if (!dataStorage.put(keys.get(i), values.get(i))) {
                        throw new IOException("Значение ключа не уникально");
                  }
            }
            reader.close();
      }

      public void writeFile() throws FileNotFoundException, IOException,
                  FileMapException {

            if (dataStorage.getSize() == 0) {
                  return;
            }

            ArrayList<Map<String, String>> dataBase = new ArrayList<Map<String, String>>();
            Iterator<Map.Entry<String, String>> it = dataStorage.getMap()
                        .entrySet().iterator();

            for (int i = 0; i < 256; i++) {
                  dataBase.add(new HashMap<String, String>());
            }
            while (it.hasNext()) {
                  Map.Entry<String, String> elem = it.next();
                  int a, b;
                  a = Math.abs(elem.getKey().hashCode() % 16);
                  b = Math.abs(elem.getKey().hashCode() / 16 % 16);
                  dataBase.get(a * 16 + b).put(elem.getKey(), elem.getValue());
            }
            for (int i = 0; i < 256; i++) {
                  if (dataBase.get(i).size() != 0) {
                        File dir = new File(dataBaseDir, String.valueOf(i / 16)
                                    + ".dir");
                        File file;
                        if (dir.exists()) {
                              if (!dir.isDirectory()) {
                                    throw new FileMapException(
                                                "База данных неподходящего формата");
                              }
                              file = new File(dir, String.valueOf(i % 16)
                                          + ".dat");
                              if (file.exists()) {
                                    if (!file.isFile()) {
                                          throw new FileMapException(
                                                      "База данных неподходящего формата");
                                    }
                              } else {
                                    file.createNewFile();
                              }
                        } else {
                              dir.mkdir();
                              if (!dir.exists()) {
                                    throw new IOException(
                                                "Проблема записи в базу данных");
                              }
                              file = new File(dir, String.valueOf(i % 16)
                                          + ".dat");
                              file.createNewFile();
                        }
                        write(file, dataBase.get(i));
                  }
            }
            dataStorage.clear();
      }

      public void write(File dataBaseFile, Map<String, String> storage)
                  throws FileNotFoundException, IOException {
            RandomAccessFile writer = new RandomAccessFile(dataBaseFile, "rw");
            writer.setLength(0);

            Integer offset = countOffset(storage);
            ArrayList<String> values = new ArrayList<String>();

            Iterator<Map.Entry<String, String>> it = storage.entrySet()
                        .iterator();

            while (it.hasNext()) {
                  Map.Entry<String, String> elem = it.next();
                  writer.write(elem.getKey().getBytes("UTF-8"));
                  writer.write("\0".getBytes("UTF-8"));
                  writer.writeInt(offset);
                  offset += elem.getValue().getBytes("UTF-8").length;
                  values.add(elem.getValue());
            }
            for (int i = 0; i < values.size(); i++) {
                  writer.write(values.get(i).getBytes("UTF-8"));
            }

            writer.close();
      }

      private Integer countOffset(Map<String, String> storage)
                  throws IOException {
            Integer curOffset = 0;
            Iterator<Map.Entry<String, String>> it = storage.entrySet()
                        .iterator();
            while (it.hasNext()) {
                  curOffset += it.next().getKey().getBytes("UTF-8").length + 1 + 4;
            }

            return curOffset;

      }

}