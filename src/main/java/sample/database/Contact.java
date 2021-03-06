package sample.database;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public abstract class Contact {

    private static final String FILEPATH = "contacts.ser";

    public static void saveContact(String name, String value){

        Path path = Paths.get(FILEPATH);
        HashMap<String, String> hmap;

        if (Files.exists(path))
            hmap = getContacts();
        else
            hmap = new HashMap<>();

        hmap.put(name, value);

        try {
            FileOutputStream fos = new FileOutputStream(FILEPATH);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hmap);
            oos.close();
            fos.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    public static HashMap<String, String> getContacts(){

        HashMap<String, String> hmap = null;
        try
        {
            FileInputStream fis = new FileInputStream(FILEPATH);
            ObjectInputStream ois = new ObjectInputStream(fis);
            hmap = (HashMap<String,String>) ois.readObject();
            ois.close();
            fis.close();
        }catch(IOException e){
            e.printStackTrace();
            return hmap;
        }catch(ClassNotFoundException c){
            c.printStackTrace();
            return hmap;
        }
        return hmap;
    }

    public static String getContactValue(String name){
        HashMap<String, String> hmap = getContacts();
        return hmap.get(name);
    }

}
