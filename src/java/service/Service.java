/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import static java.lang.System.out;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


import net.ucanaccess.jdbc.UcanaccessConnection;
import net.ucanaccess.jdbc.UcanaccessDriver;
import org.json.JSONException;

/**
 * Сервіс для вибірки даних із БД "Деканат"
 * @author Синєпольський І. В.
 */
@WebServlet(name = "Service", urlPatterns = {"/Service"})
public class Service extends HttpServlet {

  /**
   * Файл збережених sql-запитів у вигляді json-масиву об`єктів виду (приклад):
   * <code>{queryname: "назва запиту",<br/> 
   * _select: "NAMESTUDENT & ' (' & DATER & ')' AS _INFO,INDEX,NYEAR0",<br/>
   * _from: "STUDENT", <br/>
   * _where: "NYEAR0=2014",<br/> 
   * _group: "", <br/>
   * _order: "_INFO"}</code>
   *
   */
  protected String queries_storage_file = "queries.json";

  /**
   * Директорія (абсолютний шлях), де зберігаються файли баз даних .mdb
   */
  protected String absolute_path = "D:/ISKT/DEKANAT/";

  /**
   * Список хешів виду <code>"факультет" => назва_файлу_БД.mdb</code>
   */
  protected ArrayList < HashMap<String,String> > databases = new ArrayList();

  /**
   * Хеш-мапа виду <code>"parameter_name" => "parameter_value"</code> REQUEST-параметрів
   */
  protected HashMap<String,String> params = new HashMap();

  /**
   * З`єднання з БД (mdb-файл MS Access)
   */
  protected Connection conn;
  
  /**
   * Мапа з`єднань з БД (mdb-файлами MS Access)
   */
  protected HashMap<String,Connection> conns = new HashMap<>();
  
  /**
   * Максимальна кількість з`єднань з БД (mdb-файлами MS Access)
   */
  protected final int max_pool_size = 5;
  
  /**
   * Кількість з`єднань з БД (mdb-файлами MS Access)
   */
  protected int pool_size = 0;
  
  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws ClassNotFoundException if ... clear
   * @throws SQLException if SQL query goes wrong
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException, ClassNotFoundException, SQLException, JSONException {
    
    request.setCharacterEncoding("UTF-8");
    String callback = request.getParameter("callback");
    if (callback.isEmpty()){
      response.setContentType("text/json;charset=UTF-8");
    } else {
      //якщо надійшов callback, то очікується тип даних jsonp
      response.setContentType("text/javascript;charset=UTF-8");
    }
    try (PrintWriter out = response.getWriter()) {
      Map<String, String[]> parameters = request.getParameterMap();
      this.params.clear();
      for(String parameter : parameters.keySet()) {
        if (!request.getParameter(parameter).isEmpty()){
          this.params.put(parameter, request.getParameter(parameter));
        }
      }
      if (this.params.containsKey("_$action")){
        String action = this.params.get("_$action");
        String mdb_file = "";
        String table = "";
        String queryname = "";
        String _select="", _from="", _where="", _group="", _order="";
        if (this.params.containsKey("_$mdb_file")){
          mdb_file = this.params.get("_$mdb_file");
        }
        if (this.params.containsKey("_$table")){
          table = this.params.get("_$table");
        }
        if (this.params.containsKey("_$select")){
          _select = this.params.get("_$select");
        }
        if (this.params.containsKey("_$from")){
          _from = this.params.get("_$from");
        }
        if (this.params.containsKey("_$where")){
          _where = this.params.get("_$where");
        }
        if (this.params.containsKey("_$group")){
          _group = this.params.get("_$group");
        }
        if (this.params.containsKey("_$order")){
          _order = this.params.get("_$order");
        }
        if (this.params.containsKey("_$queryname")){
          queryname = this.params.get("_$queryname");
        }
        switch (action){
          case "jDatabaseList":
            if (callback.isEmpty()){
              out.println(this.jDatabaseList());
            } else {
              out.println(this.params.get("callback") 
                      + "("+ this.jDatabaseList() + ");");
            }
            break;
          case "jSavedQueries":
            if (callback.isEmpty()){
              out.println(this.jSavedQueries());
            } else {
              out.println(this.params.get("callback") 
                      + "("+ this.jSavedQueries() + ");");
            }
            break;
          case "jDb":
            if (callback.isEmpty()){
              out.println(this.jDb(mdb_file));
            } else {
              out.println(this.params.get("callback") 
                      + "("+ this.jDb(mdb_file) + ");");
            }
            break;
          case "jTables":
            if (callback.isEmpty()){
              out.println(this.jTables(mdb_file));
            } else {
              out.println(this.params.get("callback") 
                      + "("+ this.jTables(mdb_file) + ");");
            }
            break;
          case "jColumns":
            if (callback.isEmpty()){
              out.println(this.jColumns(mdb_file,table));
            } else {
              out.println(this.params.get("callback") 
                      + "("+ this.jColumns(mdb_file,table) + ");");
            }
            break;
          case "jSearchSql":
            if (callback.isEmpty()){
              out.println(this.jSearchSql(mdb_file,_select,_from,_where,_group,_order,queryname));
            } else {
              out.println(this.params.get("callback") 
                      + "("
                      + this.jSearchSql(mdb_file,_select,_from,_where,_group,_order,queryname)
                      + ");");
            }
            break;
        }
      }
    }
  }
  
  /**
   * Ініціалізація сервлету
   * @throws ServletException
   */
  @Override
  public void init() throws ServletException {
    try {
        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
    } catch (ClassNotFoundException ex) {
        Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        return ;
    }
    HashMap <String,String> ini_data = new HashMap<String,String>();
    ini_data.put("Біологічний",this.absolute_path+"Gb321.mdb");
    ini_data.put("Економічний",this.absolute_path+"Gb511.mdb");
    ini_data.put("Економічний (2)",this.absolute_path+"Gb512.mdb");
    ini_data.put("Історичний",this.absolute_path+"Gb531.mdb");
    ini_data.put("Математичний",this.absolute_path+"Gb521.mdb");
    ini_data.put("Факультет журналістики","");
    ini_data.put("Факультет іноземної філології",this.absolute_path+"Gb231.mdb");
    ini_data.put("Факультет іноземної філології (2)",this.absolute_path+"Gb232.mdb");
    ini_data.put("Факультет менеджменту",this.absolute_path+"Gb641.mdb");
    ini_data.put("Факультет соціальної педагогіки та психології",this.absolute_path+"Gb225.mdb");
    ini_data.put("Факультет соціології та управління",this.absolute_path+"Gb631.mdb");
    ini_data.put("Факультет фізичного виховання",this.absolute_path+"Gb421.mdb");
    ini_data.put("Фізичний",this.absolute_path+"Gb131.mdb");
    ini_data.put("Філологічний",this.absolute_path+"Gb221.mdb");
    ini_data.put("Юридичний",this.absolute_path+"Gb523.mdb");
    ini_data.put("Юридичний (2)",this.absolute_path+"Gb524.mdb");

    for (Map.Entry pairs : ini_data.entrySet()) {
      String _db = (String)pairs.getValue();
      HashMap <String,String> m = new HashMap<String,String>();
      String _exists = "not exists";
      m.put("faculty", (String)pairs.getKey());
      m.put("db", _db);
      if (!_db.isEmpty()){
        File f = new File(_db);
        try {
            if(f.exists() && !f.isDirectory()) {
              _exists = "exists";
            } else {
              _exists = "not exists";
            }
        }
        catch (NullPointerException ex) {
          Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      m.put("exists",_exists);
      this.databases.add(m);
    }
    Collections.sort(this.databases, new Comparator<HashMap<String,String>>() {
      @Override
      public int compare(HashMap<String, String> t1, HashMap<String, String> t2) {
        String k1,k2;
        k1 = t1.get("faculty");
        k2 = t2.get("faculty");
        return k1.compareToIgnoreCase(k2);
      }
    });
  }
  
  /**
   * Завершення сервлету
   */
  @Override
  public void destroy() {
    out.println("Destroying of servlet ......");
    try {
      for(String parameter : this.conns.keySet()) {
        Connection cn = this.conns.get(parameter);
        if (cn != null){
          cn.close();
          ((UcanaccessConnection) cn).unloadDB();
        }
      }
    } catch (SQLException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Підключення до БД
   * @param mdb_file mdb-файл (без шляху до нього!)
   * @return булевий параметр: чи вдалося з`єднатися з БД
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws IOException
   */
  protected boolean accessConnect(String mdb_file) throws ClassNotFoundException, SQLException, IOException{
    Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
    String pathDB = mdb_file, nm = "$$$@$$$";
    Connection existing_conn = null, new_conn = null;
    flog("D:/soft/apache_tomcat_out.txt",true,"------>>>");
    flog("D:/soft/apache_tomcat_out.txt",true,
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
            +"> Trying to connect('"+mdb_file+"')...");
    if (this.conn != null){
      nm = ((UcanaccessConnection) this.conn).getDbIO().getFile().getName();
    } else {
      flog("D:/soft/apache_tomcat_out.txt",true,
              new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
              +"> /this is first trying");
    }
    if (this.conns.containsKey(pathDB)){
      existing_conn = this.conns.get(pathDB);
      flog("D:/soft/apache_tomcat_out.txt",true,
              new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
              +"> '"+mdb_file+"' already exists in hashmap "+this.conns.toString());
    }
    if ( ((!mdb_file.contains(nm)) || this.conn == null)){
      flog("D:/soft/apache_tomcat_out.txt",true,
              new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
              +"> '"+mdb_file+"' not contains '"+nm+"', continue ...");
      if (!pathDB.isEmpty() && existing_conn == null){
        this.pool_size++;
        if (this.pool_size > this.max_pool_size){
          Map.Entry<String, Connection> entry = this.conns.entrySet().iterator().next();
          flog("D:/soft/apache_tomcat_out.txt",true,
                  new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
                  +"> Connection pool overflow: connection for '"+entry.getKey()+"' removed.");
          this.conns.get(entry.getKey()).close();
          ((UcanaccessConnection) this.conns.get(entry.getKey())).unloadDB();
          this.conns.remove(entry.getKey());
          this.pool_size--;
        }
        flog("D:/soft/apache_tomcat_out.txt",true,
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
                +"> '"+pathDB+"' is not empty and there is no existing connection.");
        String url = UcanaccessDriver.URL_PREFIX 
                + pathDB
                +";newDatabaseVersion=V2003"
                +";memory=false";
        new_conn = DriverManager.getConnection(url);
        this.conns.put(pathDB, new_conn);
        this.conn = new_conn;
        flog("D:/soft/apache_tomcat_out.txt",true,
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
                +"> Connected to '"+pathDB+"'.");
      } 
      if ( ((mdb_file.contains(nm)) && this.conn != null) && existing_conn == null) {
        flog("D:/soft/apache_tomcat_out.txt",true,
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
                +"> Connection for '"+pathDB+"' was not affected.");
      } 
      if (existing_conn != null){
        this.conn = existing_conn;
        flog("D:/soft/apache_tomcat_out.txt",true,
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
                +"> Connected to old connection ('"
                + ((UcanaccessConnection) this.conn).getDbIO().getFile().getName()
                +"').");
      }
    } else {
      flog("D:/soft/apache_tomcat_out.txt",true,
        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())
        +"> Connection for '"+pathDB+"' was not affected.");
    }
    return true;
  }
  
  /**
   * Збереження запиту у вигляді json
   * @param json_packed_query 
   *  <code>{ "queryname":"custom name",<br/>
   *    "_select":"SELECT ...(only columns)",<br/>
   *    "_from":"FROM ...(only tables)",<br/>
   *    "_where":"WHERE ...(only conditions)",<br/>
   *    "_group":"GROUP BY ...(only group statements)",<br/>
   *    "_order":"ORDER BY ...(only order statements)"<br/>
   *  }</code>
   * @return boolean
   */
  protected boolean saveQuery(JSONObject json_packed_query){
    try {
      String r;
      JSONObject jo = json_packed_query;
      File qF = new File(this.queries_storage_file);
      if(!qF.exists()) {
          qF.createNewFile();
      }
      BufferedReader fr = new BufferedReader(
        new InputStreamReader(
         new FileInputStream(this.queries_storage_file), Charset.forName("UTF8")
        ) 
      );
      r = fr.readLine();
      try {
        if (r == null || !r.contains("[")){
          r = "[]";
        }
      } catch (NullPointerException e) {
        Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, e);
        r = "[]";
      }
      JSONArray ja = new JSONArray(r);
      boolean already_exists = false;
      for (int i = 0; i < ja.length(); i++){
        JSONObject t; 
        String s1,s2;
        t = (JSONObject)ja.get(i);
        s1 = (String)t.get("queryname"); 
        s2 = (String)jo.get("queryname"); 
        if (s1.compareToIgnoreCase(s2) == 0){
          already_exists = true;
        }
      }
      fr.close();
      if (already_exists){
        return false;
      }
      PrintWriter fout = new PrintWriter(new BufferedWriter(
        new OutputStreamWriter(
         new FileOutputStream(this.queries_storage_file, false), Charset.forName("UTF8")
        ) 
      ));
      ja.put(jo);
      fout.println(ja);
      fout.close();
    }catch (IOException e) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, e);
      return false;
    } catch (JSONException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    }
    return true;
  }
  
  /**
   * 
   * @param from_query from-частина sql-запиту, включно із словом "from"
   * @return список хеш-мап виду <code>"назва стовпчика" => "тип"</code>
   * @throws SQLException
   */
  protected ArrayList <HashMap<String,String>> getColumnsInfo(String from_query) throws SQLException{
    Statement st = null;
    ArrayList <HashMap<String,String>> columnInfo =  new ArrayList();
    try {
      st = this.conn.createStatement();
      ResultSet rs = st.executeQuery("select top 1 * "+from_query);
      ResultSetMetaData md = rs.getMetaData();
      int columns = md.getColumnCount();
      while (rs.next()) {
        for(int i=1; i<=columns; ++i){
          HashMap<String,String> col = new HashMap();
          col.put((String)md.getColumnName(i), (String)md.getColumnTypeName(i));
          columnInfo.add(col);
          //out.println(md.getColumnName(i)+" : "+md.getColumnTypeName(i));
        }
      }
    } catch(SQLException e){
      out.print(e.getMessage());
    } finally {
      if (st != null)
        st.close();
    }
    return columnInfo;
  }
  
  //----------------------SERVICE-----------------------------
  
  /**
   * Назва файлу БД
   * @param mdb_file mdb-файл (без шляху до нього!)
   * @return json-об`єкт виду (приклад) <code>{name: "db_name.mdb"} або {"error" : "..."}</code>
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws JSONException
   * @throws IOException
   */
  protected JSONObject jDb(String mdb_file) throws ClassNotFoundException, SQLException, JSONException, IOException{
    String nm ;
    JSONObject jo = new JSONObject();
    if (!this.accessConnect(mdb_file)){
      jo.put("error","Помилка з`єднання до бази даних "+mdb_file);
      return jo;
    }
    nm = ((UcanaccessConnection) this.conn).getDbIO().getFile().getName();
    jo.put("name",nm);
    return jo;
  }
  
  /**
   * Список факультетів і повязаних з ними баз даних
   * @return json-масив із об`єктів виду <code>{"факультет": "назва_файлу_БД.mdb"}</code>
   * @throws ClassNotFoundException
   */
  protected JSONArray jDatabaseList() throws ClassNotFoundException{
    JSONArray jsArray = new JSONArray();
    for (int i = 0; i < this.databases.size(); i++){
      jsArray.put(this.databases.get(i));
    }
    return jsArray;
  }
  
  /**
   * Список таблиць БД
   * @param mdb_file mdb-файл (без шляху до нього!)
   * @return json-об`єкт виду <code>{"tables":["table1","table2",...]}</code>
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws JSONException
   * @throws IOException
   */
  protected JSONObject jTables(String mdb_file) throws ClassNotFoundException, SQLException, JSONException, IOException{
    JSONArray ja = new JSONArray();
    JSONObject jo = new JSONObject();
    if (!this.accessConnect(mdb_file)){
      jo.put("error","Помилка з`єднання до бази даних "+mdb_file);
      return jo;
    }
    Statement st = null;
    try {
      String query = "SELECT * FROM information_schema.tables where IS_INSERTABLE_INTO='YES' ORDER BY TABLE_NAME";
      st = this.conn.createStatement();
      ResultSet rs = st.executeQuery(query);
      ResultSetMetaData md = rs.getMetaData();
      int columns = md.getColumnCount();
      while (rs.next()) {
        HashMap<String,String> row = new HashMap(columns);
        for(int i=1; i<=columns; ++i){
          row.put((String)md.getColumnName(i), (String)rs.getObject(i));
        }
        ja.put(row.get("TABLE_NAME"));
      }
      jo.put("tables",ja);
    } catch(SQLException e){
      jo.put("error",e.getMessage());
    } finally {
      if (st != null)
        st.close();
    }
    return jo;
  }
  
  /**
   * Стовпчики вказаної таблиці БД
   * @param mdb_file mdb-файл (без шляху до нього!)
   * @param table назва таблиці
   * @return json-об`єкт виду <code>{"columns": [{"name":"назва стовпчика","type":"тип"},{...},...]}</code>
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws JSONException
   * @throws IOException
   */
  protected JSONObject jColumns(String mdb_file, String table) throws ClassNotFoundException, SQLException, JSONException, IOException{
    JSONArray ja = new JSONArray();
    JSONObject jo = new JSONObject();
    ArrayList<HashMap<String,String>> a = new ArrayList();
    if (!this.accessConnect(mdb_file)){
      jo.put("error","Помилка з`єднання до бази даних "+mdb_file);
      return jo;
    }
    Statement st = null;
    try {
      st = this.conn.createStatement();
      ResultSet rs = st.executeQuery("select top 1 * "
              +"from "+table
      );
      ResultSetMetaData md = rs.getMetaData();
      int columns = md.getColumnCount();
      for(int i=1; i<=columns; ++i){
        HashMap<String,String> col = new HashMap();
        col.put("name",(String)md.getColumnName(i));
        col.put("type", (String)md.getColumnTypeName(i));
        a.add(col);
        //out.println(md.getColumnName(i)+" : "+md.getColumnTypeName(i));
      }
      Collections.sort(a, new Comparator<HashMap<String,String>>() {
        @Override
        public int compare(HashMap<String, String> t1, HashMap<String, String> t2) {
          String k1,k2;
          k1 = t1.get("name");
          k2 = t2.get("name");
          return k1.compareToIgnoreCase(k2);
        }
      });
      for (int i =0; i < a.size(); i++){
        ja.put(a.get(i));
      }
      jo.put("columns", ja);
    } catch(SQLException e){
      jo.put("error",e.getMessage());
    } finally {
      if (st != null)
        st.close();
    }
    return jo;
  }
  
  /**
   * Виконує запит вибірки
   * @param mdb_file mdb-файл (без шляху до нього!)
   * @param _columns select-частина sql-запиту
   * @param _from from-частина sql-запиту
   * @param _where where-частина sql-запиту
   * @param _group group-частина sql-запиту
   * @param _order order-частина sql-запиту
   * @param queryname назва запиту (якщо порожня стрічка - запит не зберігається)
   * @return json-об`єкт виду <code>{"data": [{"name":"назва стовпчика","value":"значення"},{...},...]}</code>
   * @throws JSONException
   * @throws SQLException
   * @throws ClassNotFoundException
   * @throws IOException
   */
  protected JSONObject jSearchSql(String mdb_file, 
          String _columns, String _from, 
          String _where, String _group, String _order, String queryname
    ) throws JSONException, SQLException, ClassNotFoundException, IOException{
    JSONArray ja = new JSONArray();
    JSONObject jo = new JSONObject();
    String query = "SELECT ", 
            where = _where.trim().replace("\n", " ").replace("\r"," "), 
            columns = _columns.trim().replace("\n", " ").replace("\r"," "),
            from = _from.trim().replace("\n", " ").replace("\r"," "), 
            group = _group.trim().replace("\n", " ").replace("\r"," "), 
            order = _order.trim().replace("\n", " ").replace("\r"," ");
    ArrayList <HashMap<String,String>> columnInfo;
    Statement st = null;
    if (!this.accessConnect(mdb_file)){
      jo.put("error","Помилка з`єднання до бази даних "+mdb_file);
      return jo;
    }
    columnInfo = this.getColumnsInfo("from "+from);
    if (where.trim().isEmpty()){
      where = "TRUE ";
    }
    if (columns.isEmpty()){
      jo.put("error", "відсутні стовпчики для вибірки");
      return jo;
    }
    if (from.isEmpty()){
      jo.put("error", "відсутні таблиці для вибірки");
      return jo;
    }
    //додавання ку критерій пошуку значень параметрів, що прийшли GET чи POST 
    //  запитом, якщо їх назви співпадають із назвою стовпчиків
    for(String key : this.params.keySet()) {
      for (HashMap<String, String> columnInfoHash : columnInfo) {
        if (columnInfoHash.containsKey(key)) {
          if (columnInfoHash.get(key).contains("CHAR")) {
            where += " AND "+key + " LIKE '%"+this.params.get(key).replace("'", "\\'")+"%'";
          } else if (columnInfoHash.get(key).contains("TEXT")) {
            where += " AND "+key + " LIKE '%"+this.params.get(key).replace("'", "\\'")+"%'";
          } else if (columnInfoHash.get(key).contains("TIMESTAMP")) {
            where += " AND "+key + " LIKE '%"+this.params.get(key).replace("'", "\\'")+"%'";
          } else if (columnInfoHash.get(key).contains("DATE")) {
            where += " AND "+key + " LIKE '%"+this.params.get(key).replace("'", "\\'")+"%'";
          } else if (columnInfoHash.get(key).contains("INT")) {
            where += " AND "+key + "="+this.params.get(key);
          } else {
            where += " AND "+key + "="+this.params.get(key);
          }
        }
      }
    }
    //query += columns + " FROM "+from+" WHERE "+where; // - такий вид запиту 
    //                                                       не підтримує псевдоніми 
    //                                                       у WHERE-частині
    query += " * FROM (SELECT " + columns + " FROM "+from+") as _t_alias WHERE "+where;
    if (!group.isEmpty()){
      query += " GROUP BY "+group;
    }
    if (!order.isEmpty()){
      query += " ORDER BY "+order;
    }
    if (!queryname.trim().isEmpty()){
      //якщо вказано назву запиту, то зберегти
      JSONObject json_packed_query = new JSONObject();
      json_packed_query.put("queryname",queryname.trim());
      json_packed_query.put("_select",columns);
      json_packed_query.put("_from",from);
      json_packed_query.put("_where",where);
      json_packed_query.put("_group",group);
      json_packed_query.put("_order",order);
      this.saveQuery(json_packed_query);
    }
    try { //виконання запиту і отримання результатів
      st = this.conn.createStatement();
      ResultSet rs = st.executeQuery(query);
      ResultSetMetaData md = rs.getMetaData();
      int columns_count = md.getColumnCount();
      while (rs.next()) {
        JSONArray _jrow = new JSONArray();
        for(int i=1; i<=columns_count; ++i){
          JSONObject pair = new JSONObject();
          try {
            String col = md.getColumnName(i);
            pair.put("name",col);
            if (rs.getObject(i) != null){
              pair.put("value",rs.getObject(i));
            } else {
              pair.put("value","без значення");
            }
            _jrow.put(pair);
          } catch (NullPointerException e){
            jo.put("error",e.getMessage());
            return jo;
          }
        }
        ja.put(_jrow);
      }
      jo.put("data",ja);
    } catch(SQLException e){
      jo.put("error",e.getMessage());
      out.println(e.getMessage());
      return jo;
    } finally {
      if (st != null)
        st.close();
    }
    return jo;
  }
  
  /**
   * Збережені запити
   * @return json-масив визначеного виду
   * @see вид json-масиву надано в описі атрибуту queries_storage_file
   * @throws JSONException
   */
  protected JSONArray jSavedQueries() throws JSONException{
    //out.println("jSavedQueries --> ...");
    try {
      String r;
      BufferedReader fr = new BufferedReader(
        new InputStreamReader(
         new FileInputStream(this.queries_storage_file), Charset.forName("UTF8")
        ) 
      );
      r = fr.readLine();
      if (r == null || !r.contains("[")){
        r = "[]";
      }
      JSONArray ja = new JSONArray(r);
      fr.close();
      return ja;
    } catch (IOException e) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, e);
    } catch (JSONException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    } catch (NullPointerException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    }
    return new JSONArray("[]");
  }
  
  protected void flog(String where, boolean append, Object what) throws FileNotFoundException{
    PrintWriter fout = new PrintWriter(new BufferedWriter(
      new OutputStreamWriter(
       new FileOutputStream(where, append), Charset.forName("UTF8")
      ) 
    ));
    fout.println(what);
    fout.close();
  }



  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    try {
      processRequest(request, response);
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    } catch (SQLException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    } catch (JSONException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    try {
      processRequest(request, response);
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    } catch (SQLException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    } catch (JSONException ex) {
      Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>

}
