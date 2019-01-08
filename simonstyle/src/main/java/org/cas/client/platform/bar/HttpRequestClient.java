package org.cas.client.platform.bar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JOptionPane;

import org.cas.client.platform.bar.dialog.BarFrame;
import org.cas.client.platform.casutil.L;
import org.json.JSONException;

public class HttpRequestClient extends Thread{
	public static final String SERVER_URL = "http://test.sharethegoodones.com";
	//public static final String SERVER_URL = "http://test.sharethegoodones.com/taostyle";
	public String url;
	public String method;
	public String responseString;
	public ActionListener actionListener;
	private String jsonstr;

    public static final String KEY_SHOP_XML = "KEY_SHOP_XML";
    public static final String KEY_SHOP_LIST = "KEY_SHOP_LIST";
    public static final String KEY_SHOP_ID = "KEY_SHOP_ID";
    private static final String KEY_PREFIX_SHOP_ID_ = "KEY_SHOP_ID_";
    private static final String KEY_SHOP_NAME = "shopName";
    public static final String KEY_CUST_LAST_CHAR = "KEY_CUST_LAST_CHAR";

    public HttpRequestClient(String url, String method, String jsonstr, ActionListener actionListener) {
    	this.url = url;
    	this.method = method;
    	this.jsonstr = jsonstr;
    	this.actionListener = actionListener;
    }

    @Override
    public void run() {
        super.run();
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = prepareConnection(url, method);
            writeOut(urlConnection, jsonstr);
            
            urlConnection.setConnectTimeout(20000);
            if(urlConnection.getResponseCode()==HttpURLConnection.HTTP_OK){//得到服务端的返回码是否连接成功
                responseString = readBackFromConnection(urlConnection);
                if(actionListener != null) {
                	actionListener.actionPerformed(new ActionEvent(this, -1, responseString));
                }
            }else{
            	BarFrame.setStatusMes("net error when asking for lastest order.");
            }
        } catch (Exception e) {
            L.e("AppData USER Request failed", "url:" + url, e);
            JOptionPane.showMessageDialog(null, "User request failed with code 520, please contact us at info@ShareTheGoodOnes.com");
        }finally{
            if(urlConnection != null)
                urlConnection.disconnect();//使用完关闭TCP连接，释放资源
        }
    }

    public static String readBackFromConnection(HttpURLConnection urlConnection) throws IOException, JSONException {
        //------------字节流读取服务端返回的数据------------
        //InputStream in = urlConnection.getInputStream();//用输入流接收服务端返回的回应数据
        //BufferedInputStream bis = new BufferedInputStream(in);//高效缓冲流包装它，这里用的是字节流来读取数据的，当然也可以用字符流
        //byte[] b = new byte[1024];
        //int len = -1;
        //StringBuffer buffer = new StringBuffer();//用来接收数据的StringBuffer对象
        //while((len=bis.read(b))!=-1){
        //buffer.append(new String(b, 0, len));//把读取到的字节数组转化为字符串
        //}
        //in.close();
        //bis.close();
        //Log.d("zxy", buffer.toString());//{"json":true}
        //JSONObject rjson = new JSONObject(buffer.toString());//把返回来的json编码格式的字符串数据转化成json对象
        //------------字符流读取服务端返回的数据------------
        InputStream in = urlConnection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String str = null;
        StringBuffer buffer = new StringBuffer();
        while((str = br.readLine())!=null){//BufferedReader特有功能，一次读取一行数据
            buffer.append(str);
        }
        in.close();
        br.close();
        return buffer.toString();
    }
	    
	//  private static SharedPreferencesHelper getShopData(Context context) {
	//  return SharedPreferencesHelper.getCache(context, KEY_SHOP_XML);
	//}
	//
	///**
	//* 判断店铺是否存在
	//*
	//* @param context
	//* @param shopName
	//* @return true 存在,false 不存在
	//**/
	//private static boolean existShop(Context context, String shopName) {
	//  String shoplist = getShopData(context).getString(KEY_SHOP_LIST, "");
	//  return shoplist.contains(shopName);
	//}
	
	private static HttpURLConnection prepareConnection(String uri, String method) throws Exception {
	  HttpURLConnection urlConnection;
	  URL url = new URL(uri);
	  urlConnection = (HttpURLConnection) url.openConnection();//打开http连接
	  urlConnection.setConnectTimeout(3000);//连接的超时时间
	  urlConnection.setUseCaches(false);//不使用缓存
	  //urlConnection.setFollowRedirects(false);是static函数，作用于所有的URLConnection对象。
	  urlConnection.setInstanceFollowRedirects(true);//是成员函数，仅作用于当前函数,设置这个连接是否可以被重定向
	  urlConnection.setReadTimeout(3000);//响应的超时时间
	  urlConnection.setDoInput(true);//设置这个连接是否可以写入数据
	  urlConnection.setDoOutput(true);//设置这个连接是否可以输出数据
	  urlConnection.setRequestMethod(method);//设置请求的方式
	  urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");//设置消息的类型
	  urlConnection.connect();// 连接，从上述至此的配置必须要在connect之前完成，实际上它只是建立了一个与服务器的TCP连接
	  return urlConnection;
	}
	
	private static void writeOut(HttpURLConnection urlConnection, String jsonstr) throws Exception{
	  //-------------使用字节流发送数据--------------
	  //OutputStream out = urlConnection.getOutputStream();
	  //BufferedOutputStream bos = new BufferedOutputStream(out);//缓冲字节流包装字节流
	  //byte[] bytes = jsonstr.getBytes("UTF-8");//把字符串转化为字节数组
	  //bos.write(bytes);//把这个字节数组的数据写入缓冲区中
	  //bos.flush();//刷新缓冲区，发送数据
	  //out.close();
	  //bos.close();
	  //------------字符流写入数据------------
	  OutputStream out = urlConnection.getOutputStream();//输出流，用来发送请求，http请求实际上直到这个函数里面才正式发送出去
	  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));//创建字符流对象并用高效缓冲流包装它，便获得最高的效率,发送的是字符串推荐用字符流，其它数据就用字节流
	  bw.write(jsonstr);//把json字符串写入缓冲区中
	  bw.flush();//刷新缓冲区，把数据发送出去，这步很重要
	  out.close();
	  bw.close();//使用完关闭
	}

}