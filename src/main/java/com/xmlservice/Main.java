package com.xmlservice;

import com.xmlservice.config.Config;
import com.xmlservice.handler.CommandHandler;
import com.xmlservice.service.DatabaseService;
import com.xmlservice.view.InteractiveMenu;
import lombok.Data;

import java.util.Scanner;

@Data
public class Main {

    private XmlParser parser;
    private DatabaseService dbService;

    public Main(String xmlUrl, String jdbcUrl, String dbUser, String dbPassword) {
        this.parser = new XmlParser(xmlUrl);
        this.dbService = new DatabaseService(jdbcUrl, dbUser, dbPassword);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            CommandHandler.handleCommand(args, Config.getXmlUrl(), Config.getDbJdbcUrl(),
                    Config.getDbUser(), Config.getDbPassword());
        } else {
            runInteractive();
        }
    }

    private static void runInteractive() {
        try (Scanner scanner = new Scanner(System.in)) {
            InteractiveMenu menu = new InteractiveMenu(scanner, Config.getXmlUrl(), Config.getDbJdbcUrl(),
                    Config.getDbUser(), Config.getDbPassword());
            menu.run();
        }
    }
}
