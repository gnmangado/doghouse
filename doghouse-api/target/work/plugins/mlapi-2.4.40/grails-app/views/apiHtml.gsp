<%@ page session="false" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <script type="text/javascript">
            document.addEventListener('DOMContentLoaded', function() {
              // Click handler for collapsing and expanding objects and arrays
              function collapse(evt) {
                var collapser = evt.target;
               
                var target = collapser.parentNode.getElementsByClassName('collapsible');
                
                if ( ! target.length ) {
                  return;
                }
                
                target = target[0];

                if ( target.style.display == 'none' ) {
                  var ellipsis = target.parentNode.getElementsByClassName('ellipsis')[0];
                  target.parentNode.removeChild(ellipsis);
                  target.style.display = '';
                } else {
                  target.style.display = 'none';
                  
                  var ellipsis = document.createElement('span');
                  ellipsis.className = 'ellipsis';
                  ellipsis.innerHTML = ' &hellip; ';
                  target.parentNode.insertBefore(ellipsis, target);
                }
                
                collapser.innerHTML = ( collapser.innerHTML == '-' ) ? '+' : '-';
              }
              
              function addCollapser(item) {
                // This mainly filters out the root object (which shouldn't be collapsible)
                if ( item.nodeName != 'LI' ) {
                  return;
                }
                
                var collapser = document.createElement('div');
                collapser.className = 'collapser';
                collapser.innerHTML = '-';
                collapser.addEventListener('click', collapse, false);
                item.insertBefore(collapser, item.firstChild);
              }
              
              var items = document.getElementsByClassName('collapsible');
              for( var i = 0; i < items.length; i++) {
                addCollapser(items[i].parentNode);
              }
            }, false);
        </script>
        <style>
          body { font-family: "Helvetica Neue", sans-serif; font-size: 12px; margin: 0; padding: 15px 15px 15px 550px; line-height: 1.4em; } 
          .json-name { font-weight: bold; } 
          .json-null { color: red; } 
          .json-boolean { color: blue; } 
          .json-number { color: blue; } 
          .json-string { color: green; white-space: pre-wrap; } 
          .collapser { position: absolute; left: -1em; cursor: pointer; } 
          .code li { position: relative; } 
          .code li:after { content: ','; } 
          .code li:last-child:after { content: ''; } 
          .code ul { list-style: none; margin: 0 0 0 2em; padding: 0; } 
          .code { background-color: #f0f0f0; border-color:#D8DFEA; border-style:solid; border-width:1px 1px 1px 3px; margin: 0; overflow:auto; padding: 15px 10px; font-family: "Menlo", "Monaco", monospace; clear: both; line-height: 16px;
          } 

          h1 {
            font-size: 26px;
          }

          h2 {
            font-size: 18px;
          }

          h3 {
            font-size: 15px;
            margin-bottom: 5px;
          }

          section {
            display: block;
          }

          body > section {
            position: absolute;
            top: 0;
            height: 100%;
            width: 50%;
            overflow: auto;
          }

          body > section:first-child {
            left: 0;
          }
          
          body > section:last-child {
            right: 0;
          }

          section.text {

          }

          section.text > * {
            margin-left: 20px;
            margin-right: 20px;
          }

          table {
            border-collapse: collapse;
            border-width: 0;
          }

          table td {
            vertical-align: top;
            padding: 4px 6px;
            text-align: left;
          }

          table td {
            border: 1px solid #ccc;
          }
          
          table.attributes td:first-child {
            font-family: "Menlo", "Monaco", monospace;
            font-weight: bold;
          }

          ul h3 {
            font-family: "Menlo", "Monaco", monospace;
          }
          
          ul h3 span {
            color: #999;
          }
          a {
            font-weight: bold;
          }
       </style>
       <title>${t9n.tr(s:resourceInfo?.name.toString())}</title>
    </head>
    <body>
        <section>
            <div class="code">${json}</div>
        </section>
        <section class="text">
            <h1>${t9n.tr(s:resourceInfo?.name.toString())}</h1>
            <p>${t9n.tr(s:resourceInfo?.description.toString())}</p>
            <h2><%=t9n.tr(s:"Atributos", bundle:"mlapi")%></h2>
            <% def drawEnum = { enumAttrs, desc -> %>
                <ul>
                  <g:each in="${enumAttrs}" var="enumAttr">
                    <li>${enumAttr.key}: ${enumAttr.value}</li>
                  </g:each>
                </ul>
            <% } %>
            <% def drawAttributes = { attr, desc, drawAttributes -> %>
                <table class="attributes">
                  <% if(desc) { %>
                    <thead>
                        <th colspan="2">${desc}</th>
                    </thead>
                  <% } %>
                  <g:each in="${attr}" var="attribute">
                      <tr>
                        <td>${attribute.key}</td>
                        <td><%
                            if(attribute.value.getClass().equals(LinkedHashMap)) {
                                if(attribute.value.attributes) {
                                    drawAttributes attribute.value.attributes, t9n.tr(s:attribute.value.name.toString()), drawAttributes
                                } else if(attribute.value.enum) {
                                    drawEnum attribute.value.enum, t9n.tr(s:attribute.value.name.toString())
                                }
                            } else {
                                println t9n.tr(s:attribute.value.toString())
                            } %>
                        </td>
                      </tr>
                  </g:each>
                </table>
            <% } %>
            <% drawAttributes resourceInfo.attributes, null, drawAttributes %>
            <h2><%=t9n.tr(s:"Métodos permitidos", bundle:"mlapi")%></h2>
            <ul class="methods">
              <g:each in="${resourceInfo.methods}" var="method">
                  <li>
                    <h3>${method.method} <span>${method.example}</span></h3>
                    <p>${t9n.tr(s:method.description.toString())} </p>
                  </li>
              </g:each>
            </ul>
            <h2><%=t9n.tr(s:"Recursos relacionados", bundle:"mlapi")%></h2>
            <ul class="related">
              <g:each in="${resourceInfo.related_resources}" var="resource">
                <li><h3><span>${resource}</span></h3></li>
              </g:each>
            </ul>
            <%if(showDebugInfo) {%>
                <table class="variables">
                    <tbody>
                        <tr>
                            <th>Variable</th>
                            <th>Value</th>
                        </tr>
                        <tr>
                            <td>Host</td>
                            <td>${host}</td>
                        </tr>
                        <tr>
                            <td>Total Time</td>
                            <td>${difference}</td>
                        </tr>
                    </tbody>
                </table>
            <%}%>
        </section>
    </body>
</html>
