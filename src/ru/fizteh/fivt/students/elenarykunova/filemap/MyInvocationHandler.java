package ru.fizteh.fivt.students.elenarykunova.filemap;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class MyInvocationHandler implements InvocationHandler {

    private Writer myWriter;
    private Object myImplementation;
    private XMLStreamWriter xmlWriter;

    public MyInvocationHandler(Writer writer, Object implementation) {
        myWriter = writer;
        myImplementation = implementation;
    }

    private void writeIterable(Iterable array, IdentityHashMap<Object, Boolean> map) throws XMLStreamException {
        xmlWriter.writeStartElement("list");
        for (Object object : array) {
            xmlWriter.writeStartElement("value");
            if (object == null) {
                xmlWriter.writeEmptyElement("null");
            } else {
                if (object instanceof Iterable) {
                    if (map.containsKey(object)) {
                        xmlWriter.writeCharacters("cyclic");
                    } else {
                        map.put(object, true);
                        writeIterable((Iterable) object, map);
                    }
                } else {
                    xmlWriter.writeCharacters(object.toString());
                }
            }
            xmlWriter.writeEndElement();
        }
        xmlWriter.writeEndElement();
    }

    public void writeArgs(Object[] args) throws XMLStreamException {
        if (args == null || args.length == 0) {
            xmlWriter.writeEmptyElement("arguments");
        } else {
            xmlWriter.writeStartElement("arguments");
            for (Object object : args) {
                xmlWriter.writeStartElement("argument");
                if (object == null) {
                    xmlWriter.writeEmptyElement("null");
                } else if (object instanceof Iterable) {
                    writeIterable((Iterable<?>) object, new IdentityHashMap<Object, Boolean>());
                } else {
                    xmlWriter.writeCharacters(object.toString());
                }
                xmlWriter.writeEndElement();
            }
            xmlWriter.writeEndElement();
        }
    }

    public void writeToXMLWriter(Object impl, Method method, Object[] args, Object returnVal, Throwable exception)
            throws XMLStreamException {

        xmlWriter.writeStartElement("invoke");
        xmlWriter.writeAttribute("timestamp", Long.toString(System.currentTimeMillis()));
        xmlWriter.writeAttribute("class", impl.getClass().getName());
        xmlWriter.writeAttribute("name", method.getName());

        writeArgs(args);

        if (exception != null) {
            xmlWriter.writeStartElement("thrown");
            xmlWriter.writeCharacters(exception.toString());
            xmlWriter.writeEndElement();
        } else {
            if (!method.getReturnType().equals(void.class)) {
                xmlWriter.writeStartElement("return");
                if (returnVal == null) {
                    xmlWriter.writeEmptyElement("null");
                } else {
                    xmlWriter.writeCharacters(returnVal.toString());
                }
                xmlWriter.writeEndElement();
            }
        }
        xmlWriter.writeEndElement();
        xmlWriter.flush();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                return method.invoke(myImplementation, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        Object returnVal = null;
        Throwable methodException = null;
        try {
            returnVal = method.invoke(myImplementation, args);
        } catch (InvocationTargetException e) {
            methodException = e.getTargetException();
            throw methodException;
        } finally {
            try {
                StringWriter stringWriter = new StringWriter();
                XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
                xmlWriter = xmlFactory.createXMLStreamWriter(stringWriter);
                writeToXMLWriter(myImplementation, method, args, returnVal, methodException);
                myWriter.write(stringWriter.toString() + System.lineSeparator());
            } catch (Throwable e) {
                // nothing to do here
            }
        }
        return returnVal;
    }
}
