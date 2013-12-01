package ru.fizteh.fivt.students.elenarykunova.filemap;

import java.util.ArrayList;
import java.util.List;

import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;

public class MyStoreable implements Storeable {

    private final List<Class<?>> myTypes = new ArrayList<Class<?>>();
    private List<Object> myValues = new ArrayList<Object>();

    public MyStoreable(Table table) {
        for (int i = 0; i < table.getColumnsCount(); i++) {
            myTypes.add(i, table.getColumnType(i));
            myValues.add(i, null);
        }
    }

    public MyStoreable(Table table, List<?> values) throws IndexOutOfBoundsException, ColumnFormatException {
        if (values == null || values.size() != table.getColumnsCount()) {
            throw new IndexOutOfBoundsException("number of columns and size of values list mismatch");
        }
        for (int i = 0; i < table.getColumnsCount(); i++) {
            myTypes.add(i, table.getColumnType(i));
            if (values.get(i) == null) {
                myValues.add(i, null);
            } else {
                Object newVal = values.get(i);
                Object newClass = newVal.getClass();
                Object tableClass = table.getColumnType(i);
                if (tableClass.equals(Integer.class)) {
                    if (newClass.equals(Integer.class)) {
                        myValues.add(i, newVal);
                    } else if (newClass.equals(Byte.class)) {
                        myValues.add(i, Byte.class.cast(newVal).intValue());
                    } else {
                        throw new ColumnFormatException("expected " + Integer.class + " but was " + newClass);
                    }
                } else if (tableClass.equals(Long.class)) {
                    if (newClass.equals(Long.class)) {
                        myValues.add(i, newVal);
                    } else if (newClass.equals(Integer.class)) {
                        myValues.add(i, Integer.class.cast(newVal).longValue());
                    } else if (newClass.equals(Byte.class)) {
                        myValues.add(i, Byte.class.cast(newVal).longValue());
                    } else {
                        throw new ColumnFormatException("expected " + Long.class + " but was " + newClass);
                    }
                } else if (tableClass.equals(Byte.class)) {
                    if (newClass.equals(Byte.class)) {
                        myValues.add(i, newVal);
                    } else if (newClass.equals(Integer.class)) {
                        Integer current = Integer.class.cast(newVal);
                        if (current > Byte.MAX_VALUE || current < Byte.MIN_VALUE) {
                            throw new ColumnFormatException("expected " + Byte.class + " but was " + newClass);
                        }
                        myValues.add(i, current.byteValue());
                    } else if (newClass.equals(Long.class)) {
                        Long current = Long.class.cast(newVal);
                        if (current > Byte.MAX_VALUE || current < Byte.MIN_VALUE) {
                            throw new ColumnFormatException("expected " + Byte.class + " but was " + newClass);
                        }
                        myValues.add(i, current.byteValue());
                    } else {
                        throw new ColumnFormatException("expected " + Byte.class + " but was " + newClass);
                    }
                } else if (tableClass.equals(Float.class)) {
                    if (newClass.equals(Float.class)) {
                        myValues.add(i, newVal);
                    } else if (newClass.equals(Double.class)) {
                        myValues.add(i, Double.class.cast(newVal).floatValue());
                    } else if (newClass.equals(Integer.class)) {
                        myValues.add(i, Integer.class.cast(newVal).floatValue());
                    } else if (newClass.equals(Long.class)) {
                        myValues.add(i, Long.class.cast(newVal).floatValue());
                    } else {
                        throw new ColumnFormatException("expected " + Float.class + " but was " + newClass);
                    }
                } else if (tableClass.equals(Double.class)) {
                    if (newClass.equals(Double.class)) {
                        myValues.add(i, newVal);
                    } else if (newClass.equals(Float.class)) {
                        myValues.add(i, Float.class.cast(newVal).doubleValue());
                    } else if (newClass.equals(Integer.class)) {
                        myValues.add(i, Integer.class.cast(newVal).doubleValue());
                    } else {
                        throw new ColumnFormatException("expected " + Double.class + " but was " + newClass);
                    }
                } else if (tableClass.equals(Boolean.class)) {
                    if (newClass.equals(Boolean.class)) {
                        myValues.add(i, newVal);
                    } else if (newClass.equals(String.class)) {
                        String current = String.class.cast(newVal);
                        if (current.toLowerCase().equals("false")) {
                            myValues.add(i, false);
                        } else if (current.toLowerCase().equals("true")) {
                            myValues.add(i, true);
                        } else {
                            throw new ColumnFormatException("expected " + Boolean.class + " but was " + newClass);
                        }
                    } else {
                        throw new ColumnFormatException("expected " + Boolean.class + " but was " + newClass);
                    }
                } else if (tableClass.equals(String.class)) {
                    if (newClass.equals(String.class)) {
                        myValues.add(i, newVal);
                    } else {
                        throw new ColumnFormatException("expected " + String.class + " but was " + newClass);
                    }
                }
            }
        }
    }

    private boolean isCorrectIndex(int columnIndex) {
        return (columnIndex >= 0 && columnIndex < myTypes.size());
    }

    @Override
    public void setColumnAt(int columnIndex, Object value) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (value != null) {
            if (!value.getClass().isAssignableFrom(myTypes.get(columnIndex))) {
                throw new ColumnFormatException("type of value mismatches type of column : expected "
                        + myTypes.get(columnIndex) + " but was " + value.getClass());
            }
        }
        myValues.set(columnIndex, value);
    }

    @Override
    public Object getColumnAt(int columnIndex) throws IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        return myValues.get(columnIndex);
    }

    @Override
    public Integer getIntAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(Integer.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + Integer.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (Integer) myValues.get(columnIndex);
    }

    @Override
    public Long getLongAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(Long.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + Long.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (Long) myValues.get(columnIndex);

    }

    @Override
    public Byte getByteAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(Byte.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + Byte.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (Byte) myValues.get(columnIndex);
    }

    @Override
    public Float getFloatAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(Float.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + Float.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (Float) myValues.get(columnIndex);
    }

    @Override
    public Double getDoubleAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(Double.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + Double.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (Double) myValues.get(columnIndex);
    }

    @Override
    public Boolean getBooleanAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(Boolean.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + Boolean.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (Boolean) myValues.get(columnIndex);
    }

    @Override
    public String getStringAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        if (!isCorrectIndex(columnIndex)) {
            throw new IndexOutOfBoundsException("incorrect index");
        }
        if (!myTypes.get(columnIndex).equals(String.class)) {
            throw new ColumnFormatException("type of value mismatches type of column : expected " + String.class
                    + " but was " + myTypes.get(columnIndex));
        }
        return (String) myValues.get(columnIndex);
    }

    @Override
    public String toString() {
        String className = MyStoreable.class.getSimpleName();
        StringBuffer res = new StringBuffer(className + "[");

        for (int i = 0; i < myValues.size(); i++) {
            Object val = myValues.get(i);
            if (val != null) {
                Class<?> type = myTypes.get(i);
                if (type.equals(Integer.class)) {
                    res.append(String.valueOf((Integer) val));
                } else if (type.equals(String.class)) {
                    res.append(val);
                } else if (type.equals(Boolean.class)) {
                    res.append(String.valueOf((Boolean) val));
                } else if (type.equals(Float.class)) {
                    res.append(String.valueOf((Float) val));
                } else if (type.equals(Double.class)) {
                    res.append(String.valueOf((Double) val));
                } else if (type.equals(Byte.class)) {
                    res.append(String.valueOf((Byte) val));
                } else if (type.equals(Long.class)) {
                    res.append(String.valueOf((Long) val));
                } else {
                    throw new RuntimeException("unexpected type : " + myTypes.get(i).getSimpleName());
                }
            }
            if (i != myValues.size() - 1) {
                res.append(",");
            }
        }
        res.append("]");
        return res.toString();
    }

}
