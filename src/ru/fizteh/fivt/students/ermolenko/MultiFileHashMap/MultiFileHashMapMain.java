package ru.fizteh.fivt.students.ermolenko.multifilehashmap;

import java.io.File;
import java.io.IOException;

public class MultiFileHashMapMain {

    public static void main(String[] args) throws IOException {

        System.out.println("MULTI FILE");
        //String currentProperty = "/Users/evgenij/Documents/JAVA_Ex/fizteh-java-2013/src/ru/fizteh/fivt/students/ermolenko/multifilehashmap/folder/";
        String currentProperty = System.getProperty("fizteh.db.dir");
        File base = new File(currentProperty);
        if (!base.exists()) {
            base.createNewFile();
        }

        try {
            base = base.getCanonicalFile();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        MultiFileHashMap mfhm = new MultiFileHashMap(base);
        MultiFileHashMapExecutor exec = new MultiFileHashMapExecutor();

        try {
            if (args.length > 0) {
                mfhm.batchState(args, exec);
            } else {
                System.out.println("INTERACTIVE");
                mfhm.interactiveState(exec);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        System.exit(0);
    }
}
