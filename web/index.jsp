<%-- 
    Document   : index
    Created on : 19 січ 2015, 19:07:12
    Author     : Синєпольський Ігор
--%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.io.File"%>

 <% 
  DateFormat df = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  DateFormat dfr = new java.text.SimpleDateFormat("dd.MM.yyyy"); 
  //GET params
  request.setCharacterEncoding("UTF-8");
  
%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>jDEKANAT - Simple UI</title>
        <link rel="stylesheet" href="own/css/main.css" type="text/css" />
        <script type="text/javascript">
          if(typeof jQuery === "undefined"){
            document.write('<script type="text/javascript" '
             +'src="own/js/jquery-1.11.1.min.js"><\/script>');
          }
          
        </script>
        <script type="text/javascript">
          $(function(){
            db="";
            table = "";
            service_url = "http://10.1.100.203:8088/jdekanat/Service";
            faculties_html = "";
            faculties_val = "";
            _wait = false;
            
            var err_func = function(x,e){
              alert(e);
              $("#Faculties").html("");
              $("#dbTables").html("<p class='error'>помилка</p>");
              $("#Faculties").append("<option >"
                      +"помилка!"
                      +"</option>");
              $("#table_placeholder").html("<p class='error'>помилка</p>");
              _wait = false;
              $("#reload_dbtables").show();
              $("#execute_query").show();
              $("#saved_queries").show();
           };
           
            var loading_func = function(){
              faculties_html = $("#Faculties").html();
              faculties_val = $("#Faculties").val();
              $("#Faculties").html("");
              $("#Faculties").append("<option >"
                      +"зачекайте..."
                      +"</option>");
              $("#saved_queries").html("<option >"
                      +"зачекайте..."
                      +"</option>");
              $("#table_placeholder").html("");
              $("#table_placeholder").append("<i>зачекайте...</i>");
                $("#reload_dbtables").hide();
                $("#execute_query").hide();
                $("#saved_queries").hide();
              _wait = true;
           };
           
            var common_success = function(){
                $("#reload_dbtables").show();
                $("#execute_query").show();
                $("#saved_queries").show();
                _wait = false;
                $("#table_placeholder").html("");
                $("#Faculties").html(faculties_html);
                $("#Faculties").val(faculties_val);
                savedQueries();
            }
            
            var Faculties = function(){
              $.ajax({
                url: service_url,
                dataType: "jsonp",
                crossDomain: true,
                data: {_$action: "jDatabaseList"},
                success: function(data){
                  _wait = false;
                  $("#table_placeholder").html("");
                  $("#saved_queries").html("<option >"
                          +"ще не заватажено"
                          +"</option>");
                  if (typeof data.error !== "undefined"){
                    alert(data.error);
                  } else {
                    $("#Faculties").html("");
                    $("#Faculties").append("<option id='dis_opt' "
                            +">"
                            +"--Оберіть факультет--"
                            +"</option>");
                    for (var i = 0; i < data.length; i++){
                      $("#Faculties").append("<option "
                              +"value='"+data[i].db+"' "
                              +((data[i].db === db && db.length > 0)? " selected":" ")
                              +((data[i].exists === "exists")? " ":" disabled")
                              +">"
                              +data[i].faculty
                              +"</option>");
                    }
                  }
                },
                //beforeSend: loading_func,
                error : err_func,
                fail : err_func
              });
            };
            
            var savedQueries = function(){
              $.ajax({
                url: service_url,
                dataType: "jsonp",
                crossDomain: true,
                data: {_$action: "jSavedQueries"},
                success: function(data){
                  _wait = false;
                  $("#saved_queries").html("");
                  $("#reload_dbtables").show();
                  $("#execute_query").show();
                  $("#saved_queries").show();
                  if (typeof data.error !== "undefined"){
                    alert(data.error);
                  } else {
                    $("#saved_queries").html("");
                    $("#saved_queries").append("<option id='query_dis_opt'>"
                            +"--Оберіть запит--"
                            +"</option>");
                    for (var i = 0; i < data.length; i++){
                      $("#saved_queries").append("<option "
                              +"value='"+data[i].queryname.replace("'","\\'")+"' "
                              +'data-select="'+data[i]._select.replace("\"","\\\"")+'" '
                              +'data-from="'+data[i]._from.replace("\"","\\\"")+'" '
                              +'data-where="'+data[i]._where.replace("\"","\\\"")+'" '
                              +'data-group="'+data[i]._group.replace("\"","\\\"")+'" '
                              +'data-order="'+data[i]._order.replace("\"","\\\"")+'" '
                              +">"
                              +data[i].queryname
                              +"</option>");
                    }
                  }
                },
                //beforeSend: loading_func,
                error : err_func,
                fail : err_func
              });
            };
            
            var Tables = function(){
              $.ajax({
                url: service_url,
                dataType: "jsonp",
                crossDomain: true,
                data: {_$action: "jTables", _$mdb_file: db},
                success: function(data){
                  common_success();
                  if (typeof data.error !== "undefined"){
                    alert(data.error);
                  } else if (typeof data.tables === "undefined"){
                    alert("структуру `tables` не знайдено");
                  } else {
                    $("#dbTables").html("<ol></ol>");
                    for (var i = 0; i < data.tables.length; i++){
                      $("#dbTables ol").append("<li><a href='#' data-name='"+data.tables[i]+"' class='dbtables'>"
                              +data.tables[i]
                              +"</a><div id='cols_"+data.tables[i]+"'></div></li>");
                    }
                    $("#dbTables ol li a").click(function(){
                      if (db.length > 0 && !_wait){
                        $("#dis_table_opt").remove();
                        table = $(this).attr("data-name");
                        Columns();
                      }
                      return false;
                    });
                  }
                },
                beforeSend: loading_func,
                error : err_func,
                fail : err_func
              });
            };
            
            var Columns = function(){
              var div_id = "cols_"+table;
              $("#"+div_id).html("");
              $.ajax({
                url: service_url,
                dataType: "jsonp",
                crossDomain: true,
                data: {_$action: "jColumns", _$mdb_file: db, _$table: table},
                success: function(data){
                  common_success();
                  if (typeof data.error !== "undefined"){
                    alert(data.error);
                  } else if (Object.prototype.toString.call( data.columns ) !== "[object Array]"){
                    alert("структуру `columns` не знайдено");
                  } else {
                    $("#"+div_id).html("<div id='"+table+"_columns_list' class='columns-list'></div>");
                    for (var i = 0; i < data.columns.length; i++){
                      $("#"+table+"_columns_list").append("<span class='column-item'>"
                        +data.columns[i].name
                        +" <span style='vertical-align: sub; font-size: 6pt; padding-left: 1px;'>"+data.columns[i].type+"</span>"
                        +"</span>"
                      );
                    }
                    $("#"+table+"_columns_list").append("<div style='width: 100%;'></div>");
                  }
                },
                beforeSend: loading_func,
                error : err_func,
                fail : err_func
              });
            };
            
            
            var exexSelectQuery = function(){
              $.ajax({
                url: service_url,
                dataType: "jsonp",
                crossDomain: true,
                data: {_$action: "jSearchSql", _$mdb_file: db, 
                  _$select: $("#_select").val(),
                  _$from: $("#_from").val(),
                  _$where: $("#_where").val(),
                  _$group: $("#_group").val(),
                  _$order: $("#_order").val(),
                  _$queryname: $("#queryname").val()
                },
                success: function(data){
                  common_success();
                  if (typeof data.error !== "undefined"){
                    alert(data.error);
                  } else if (Object.prototype.toString.call( data.data ) !== "[object Array]"){
                    alert("структуру `data` не знайдено");
                  } else {
                    var t_id = table+'_data';
                    if (data.data.length === 0){
                      $("#table_placeholder").html("<i>Відсутні дані</i>");
                      return true;
                    }
                    $("#table_placeholder").html("<table id='"+t_id+"'></table>");
                    for (var i = 0; i < data.data.length; i++){
                      var tr_id = table+'_data_'+i;
                      if (i === 0){
                        var tr0_id = table+'_data_columns';
                        $("#"+t_id).append("<tr id='"+tr0_id+"'></tr>");
                        for (var j = 0; j < data.data[i].length; j++){
                          $("#"+tr0_id).append("<th>"+data.data[i][j].name+"</th>");
                        }
                      }
                      $("#"+t_id).append("<tr id='"+tr_id+"'></tr>");
                      for (var j = 0; j < data.data[i].length; j++){
                        var v = data.data[i][j].value;
                        if (v === "без значення"){
                          v = "<i> н/з </i>";
                        }
                        $("#"+tr_id).append("<td>"+v+"</td>");
                      }
                    }
                  }
                },
                beforeSend: loading_func,
                error : err_func,
                fail : err_func
              });
            };

            Faculties();
            
            $("#reload_faculties").click(function(){
              if (!_wait){
                Faculties();
                return false;
              }
            });
            $("#reload_dbtables").click(function(){
              if (!_wait){
                Tables();
              }
              return false;
            });
            $("#execute_query").click(function(){
              if (!_wait){
                exexSelectQuery();
              }
              return false;
            });
            
            $("#Faculties").change(function(){
              if (!_wait){
                $("#dis_opt").remove();
                db = $("#Faculties option:selected").val();
                Tables();
              }
              return false;
            });
            
            $("#saved_queries").change(function(){
              $("#query_dis_opt").remove();
              var columns = $("#saved_queries option:selected").attr('data-select');
              var from = $("#saved_queries option:selected").attr('data-from');
              var where = $("#saved_queries option:selected").attr('data-where');
              var group = $("#saved_queries option:selected").attr('data-group');
              var order = $("#saved_queries option:selected").attr('data-order');
              var queryname = $("#saved_queries option:selected").attr('value');
              
              $("#_select").val(columns);
              $("#_from").val(from);
              $("#_where").val(where);
              $("#_group").val(group);
              $("#_order").val(order);
              $("#queryname").val(queryname);
              
              if (!_wait){
                exexSelectQuery();
              }
              return false;
            });
            
            if (db.length === 0){
                $("#reload_dbtables").hide();
                $("#execute_query").hide();
                $("#saved_queries").hide();
            }
            
          });
          
        </script>
    </head>
<body>
  
  <div class="main-block">
    <legend class="main-legend">tools</legend>
    <table style="width: 99%;">
      <tr>
        <td>
          <div style='width: 200px;'>Факультети </div>
          <a href="#" id="reload_faculties">[оновити]</a>
        </td>
        <td>
          <select id="Faculties">
            <option disabled>Не завантажено ще</option>
          </select>
        </td>
      </tr>
      
      <tr>
        <td>
          <div style='width: 200px;'>Таблиці </div>
          <a href="#" id="reload_dbtables">[оновити]</a>
        </td>
        <td>
          <div id="dbTables">
            <i>Не завантажено ще</i>
          </div>
        </td>
      </tr>
      
      <tr>
        <td>
          <div style='width: 200px;'>Запити до БД  </div>
          <a href="#" id="execute_query">[виконати]</a>
          <hr/>
          <select id="saved_queries">
            <option disabled>Не завантажено ще</option>
          </select>
        </td>
        <td>
          <div id="dbQueryForm">
            <label for="_select" style="font-size: 8pt;">[select]: які атрибути вибирати * </label>
            <textarea style="width: 99%;" rows="2" id="_select"></textarea>
            <hr/>
            <label for="_from" style="font-size: 8pt;">[from]: з яких таблиць * </label>
            <textarea style="width: 99%;" rows="2" id="_from"></textarea>
            <hr/>
            <label for="_where" style="font-size: 8pt;">[where]: умови вибірки</label>
            <textarea style="width: 99%;" rows="3" id="_where"></textarea>
            <hr/>
            <label for="_group" style="font-size: 8pt;">[group by]: групування</label>
            <textarea style="width: 99%;" rows="2" id="_group"></textarea>
            <hr/>
            <label for="_order" style="font-size: 8pt;">[order by]: сортування</label>
            <textarea style="width: 99%;" rows="2" id="_order"></textarea>
            <label for="queryname" style="font-size: 8pt;">назва запиту для збереження</label>
            <input type="text" id="queryname" />
          </div>
        </td>
      </tr>
      
    </table>
  </div>
  
  <div class="main-block info-block">
    <legend class="main-legend">info</legend>
    <div id="table_placeholder"></div>
  </div>
    

<footer>
  <p class="footer">
    Сторінку сформовано  
  <%= df.format((new java.util.Date()))
  %>
  </p>
</footer>
</body>
</html>
</html>