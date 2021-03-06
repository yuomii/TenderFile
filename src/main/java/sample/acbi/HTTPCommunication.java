package sample.acbi;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import sample.classes.IPFSFile;
import sample.classes.Transaction;
import sample.crypto.CryptoUtil;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.PublicKey;

public abstract class HTTPCommunication {

    private static String SERVER = "http://localhost:46657/";

    public static String getPubKeyByHash(String hash){

        JSONObject json = null;
        try {
            json = new JSONObject(IOUtils.toString(new URL(SERVER+"tx?hash=0x"+hash), Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(json != null)
            return json.getJSONObject("result").get("tx").toString();
        else
            return "Error";
    }

    public static String registerUser(PublicKey pubKey){

        JSONObject json = null;

        String test = SERVER+"broadcast_tx_commit?tx="+"\""+CryptoUtil.publicKeyToString(pubKey)+"\"";
        try {
            json = new JSONObject(IOUtils.toString(new URL(test), Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(json != null){
            return json.getJSONObject("result").get("hash").toString();}
        else
            return null;
    }

    public static String shareIpfsFile(Transaction file){

        JSONObject json = null;
        String test = SERVER+"broadcast_tx_commit?tx="+"\""+CryptoUtil.stringFromIpfsFile(file)+"\"";
        try {
            json = new JSONObject(IOUtils.toString(new URL(test), Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(json != null)
            return json.getJSONObject("result").get("hash").toString();
        else
            return null;
    }

}
