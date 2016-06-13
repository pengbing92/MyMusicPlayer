package com.whut.httpclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class MyClient
{
    private static final String CAS_LOGIN_URL_PART = "tickets";
    private static final String CAS_LOGOUT_URL_PART = "tickets";

    private static final int REQUEST_TIMEOUT = 5*1000;
    private static final int SO_TIMEOUT = 10*1000;  

    private DefaultHttpClient httpClient;

    private String baseURL;
    
    private static MyClient instance;
    
    public DefaultHttpClient getHttpClient()
    {
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        client.getParams().setParameter(
				CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8");
        return client;
    }

    public MyClient (String baseURL)
    {
        this.httpClient = getHttpClient();
        this.baseURL = baseURL;
    }
    
    
   public static MyClient getInstance(){
    	if(instance==null){ 
    		synchronized(MyClient.class){
    			if(instance==null){
    				instance = new MyClient("");
    			}
    		}
    	}
    	return instance;
    }

    public boolean login(String username,String password,String sessionGenerateService){
        String tgt = getTGT(username, password);
        if(tgt==null){
            return false;
        }

        String st = getST( sessionGenerateService, tgt);
        if(st==null){
            return false;
        }

        String sessionId = generateSession(sessionGenerateService,st);
        if(sessionId==null){
   
            return false;
        }
        return true;
    }

    //获取HttpResponse
    private String getResponseBody(HttpResponse response){
        StringBuilder sb = new StringBuilder();
        try {
            InputStream instream = response.getEntity().getContent();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(instream), 65728);
            String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            instream.close();
        }
        catch (IOException e) { e.printStackTrace(); }
        catch (Exception e) { e.printStackTrace(); }
        return sb.toString();

    }

    /**
     *获取ticket granting ticket
     */
    public String getTGT(String username, String password)
    {
        String tgt = null;
        HttpPost httpPost = new HttpPost (baseURL + CAS_LOGIN_URL_PART);
        try
        {
            List <NameValuePair> nvps = new ArrayList <NameValuePair> ();
            nvps.add(new BasicNameValuePair ("username", username));
            nvps.add(new BasicNameValuePair ("password", password));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = getResponseBody(response);
            switch (response.getStatusLine().getStatusCode())
            {
                case 201:
                {
                    Matcher matcher = Pattern.compile(".*action='.*/tickets/(TGT-.*\\.whut\\.org).*")
                            .matcher(responseBody.replaceAll("\"", "'"));
                    if (matcher.matches()){
                        tgt = matcher.group(1);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return tgt;
    }

    public String getST(String service,String tgt){
        if (tgt == null) return null;
        String st = null;
        HttpPost httpPost = new HttpPost (baseURL + CAS_LOGIN_URL_PART+"/" + tgt);
        try
        {
            List <NameValuePair> nvps = new ArrayList <NameValuePair> ();
            nvps.add(new BasicNameValuePair ("service", service));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = getResponseBody(response);

            switch (response.getStatusLine().getStatusCode())
            {
                case 200:
                {
                    st = responseBody;
                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return st;
    }


    public String generateSession(String service,String st){
        String sessionId = "";
        HttpGet httpGet = new HttpGet (service+"?ticket="+st);
        try
        {
            HttpResponse response = httpClient.execute(httpGet);
            EntityUtils.toString(response.getEntity() ,"utf-8");
            switch (response.getStatusLine().getStatusCode())
            {
                case 200:
                {
                    List<Cookie> cookies = httpClient.getCookieStore().getCookies();
                    if (! cookies.isEmpty()){
                        for (Cookie cookie : cookies){
                            if(cookie.getName().equals("JSESSIONID")){
                                sessionId = cookie.getValue();
                                break;
                            }
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return sessionId;
    }

    //发送GET请求
    public String doGet(String service){
        Log.i("cas client doGet url:", service);
        HttpGet httpGet = new HttpGet (service);
        try
        {
            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = getResponseBody(response);
            switch (response.getStatusLine().getStatusCode())
            {
                case 200:
                {
                    Log.i("msg", responseBody);
                    return responseBody;
                }
                default:
                    break;
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 发送POST请求
     * @param service 请求地址
     * @param params  上传的数据
     * @return
     */
    synchronized public String doPost(String service,HashMap<String,String> params){
        Log.i("cas client doPost url:", service);
        HttpPost httpPost = new HttpPost (service);
        try
        {
            List <NameValuePair> nvps = new ArrayList <NameValuePair> ();
            for(String key:params.keySet()){
                nvps.add(new BasicNameValuePair (key, params.get(key)));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nvps,HTTP.UTF_8));
            

            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = getResponseBody(response);
            switch (response.getStatusLine().getStatusCode())
            {
                case 200:
                {
                    Log.i("msg", responseBody);
                    return responseBody;
                }
                default:
                    break;
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }


    public boolean logout ()
    {
        boolean logoutSuccess = false;
        HttpDelete httpDelete = new HttpDelete(baseURL + CAS_LOGOUT_URL_PART);
        try
        {
            HttpResponse response = httpClient.execute(httpDelete);
            logoutSuccess = (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
        }
        catch (Exception e)
        {
            logoutSuccess = false;
        }
        return logoutSuccess;
    }


    public boolean reset(){
    	instance = null;
    	boolean result = false;
    	result = logout();
    	return result;
    }
    
    
    // 发送post请求
    synchronized public String doPost(String service) {
    	Log.i("cas client doPost url:", service);
        HttpPost httpPost = new HttpPost (service);
        try
        {
         
            HttpResponse response = httpClient.execute(httpPost);
  
            
            InputStream inputStream = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream),65728);
            
            String line = null;
            StringBuilder sb = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            inputStream.close();
            
            Log.i("response_status", response.getStatusLine().getStatusCode()+"");
            
            switch (response.getStatusLine().getStatusCode())
            {
                case 200:
                {
                    Log.i("cas client doPost response:", sb.toString());
                    return sb.toString();
                }
                default:
                    break;
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
    
    
    /**
     * 把网络歌词写入保存到本地文件中
     * @param downloadUrl 下载地址
     * @param songName    歌曲名
     */
    public boolean createPrcFile(String rootPath, String downloadUrl, String songName) {
    	
    	HttpGet httpGet = new HttpGet(downloadUrl);
    	
    	try {
			HttpResponse httpResponse = httpClient.execute(httpGet);
			
			InputStream inputStream = httpResponse.getEntity().getContent();
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream),65728);
			
			// 创建本地文件
			String filePath = rootPath + "/" + songName + ".prc";		
			File file = new File(filePath);
			boolean created = false;
			if (!file.exists()) {
				created = file.createNewFile();
			}
			
			String line = null;
			FileWriter fileWriter  = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			while ((line = bufferedReader.readLine()) != null) {
				if (created) {
					
					Log.i("line", line);
					bufferedWriter.write(line);
					bufferedWriter.newLine();  // 换行
				}		
			}
			
			// 释放资源
			bufferedReader.close();
			inputStream.close();
			bufferedWriter.close();
			fileWriter.close();
			
			switch (httpResponse.getStatusLine().getStatusCode())
            {
                case 200:
                {
                    return true;
                }
                default:
                    break;
            }
				
		} catch (ClientProtocolException e) {		
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
    	
    	return false;
    }
    
    

}