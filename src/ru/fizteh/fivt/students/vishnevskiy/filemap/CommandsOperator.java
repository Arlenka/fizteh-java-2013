package ru.fizteh.fivt.students.vishnevskiy.filemap;

import ru.fizteh.fivt.students.vishnevskiy.filemap.commands.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;

public class CommandsOperator {
    private Map<String, Command> commandsTable = new HashMap<String, Command>();
    private SingleFileMap singleFileMap;

    private void loadClasses() {
        Exit exit = new Exit();
        commandsTable.put(exit.getName(), exit);
        Put put = new Put();
        commandsTable.put(put.getName(), put);
        Get get = new Get();
        commandsTable.put(get.getName(), get);
        Remove remove = new Remove();
        commandsTable.put(remove.getName(), remove);
    }

    public CommandsOperator() {
        try {
            loadClasses();
            File datebase = new File(System.getProperty("fizteh.db.dir") + "/db.dat");
            if (!datebase.isAbsolute()) {
                throw new IOException("Wrong path to datebase file");
            }
            singleFileMap = new SingleFileMap(new File(System.getProperty("fizteh.db.dir") + "/db.dat"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public int runCommand(String line) {
        try {
//            line = line.replaceAll("\\s+", " ").trim();
//            String[] commandAndArgs = line.split(" ");
//            String commandName = commandAndArgs[0];
//            String[] args = Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.length);
            int i = 0;
            while (line.charAt(i) == ' ') {
                ++i;
            }
            line = line.substring(i);
            String commandName = line.split("\\s", 2)[0];
            Command command = commandsTable.get(commandName);
            if (command == null) {
                if (commandName.equals("")) {
                    return 0;
                } else {
                    throw new FileMapException(commandName + ": command not found");
                }
            }
            String[] args = line.split("\\s+", command.getArgsNum() + 1);
            i = 1;
            while ((i < args.length) && (!args[i].isEmpty())) {
                ++i;
            }
            args = Arrays.copyOfRange(args, 1, i);
            command.execute(singleFileMap, args);
        } catch (FileMapException e) {
            System.err.println(e.getMessage());
            System.err.flush();
            return 1;
        }
        return 0;
    }
}
