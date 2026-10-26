/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 93.69884085040927, "KoPercent": 6.301159149590727};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.8309344613525244, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [1.0, 500, 1500, "C1 GET /clientes"], "isController": false}, {"data": [1.0, 500, 1500, "setUp POST /produtos B"], "isController": false}, {"data": [1.0, 500, 1500, "setUp POST /produtos A"], "isController": false}, {"data": [0.1722689075630252, 500, 1500, "C3 GET /produtos"], "isController": false}, {"data": [0.9997813729777001, 500, 1500, "C1 GET /produtos?nome="], "isController": false}, {"data": [0.9993506493506493, 500, 1500, "C1 GET /produtos/{id}"], "isController": false}, {"data": [0.0, 500, 1500, "C3 GET /vendas (lista completa)"], "isController": false}, {"data": [1.0, 500, 1500, "C2 GET /vendas/{id}"], "isController": false}, {"data": [0.6071163825055597, 500, 1500, "C2 POST /vendas"], "isController": false}, {"data": [0.12320916905444126, 500, 1500, "C3 POST /vendas"], "isController": false}, {"data": [0.9997827031725337, 500, 1500, "C1 GET /produtos/{id}/estoque"], "isController": false}, {"data": [0.16902313624678664, 500, 1500, "C3 GET /produtos/{id}/estoque"], "isController": false}, {"data": [1.0, 500, 1500, "setUp POST /clientes"], "isController": false}, {"data": [0.9995684074233924, 500, 1500, "C1 GET /produtos"], "isController": false}, {"data": [0.0, 500, 1500, "tearDown GET estoque produto A"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 18203, 1147, 6.301159149590727, 658.8322803933415, 5, 24876, 23.0, 1753.0, 4912.399999999998, 12653.679999999971, 91.21293206257579, 271.9495298939323, 18.725169751172544], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["C1 GET /clientes", 2282, 0, 0.0, 37.2695004382121, 5, 493, 13.0, 87.0, 168.0, 397.3600000000006, 39.059959262619174, 42.482633391386955, 7.056730921469284], "isController": false}, {"data": ["setUp POST /produtos B", 1, 0, 0.0, 45.0, 45, 45, 45.0, 45.0, 45.0, 45.0, 22.22222222222222, 8.3984375, 7.248263888888889], "isController": false}, {"data": ["setUp POST /produtos A", 1, 0, 0.0, 62.0, 62, 62, 62.0, 62.0, 62.0, 62.0, 16.129032258064516, 6.048387096774194, 5.213583669354839], "isController": false}, {"data": ["C3 GET /produtos", 833, 0, 0.0, 4285.319327731092, 8, 15583, 4194.0, 8730.2, 12633.999999999998, 15313.66, 11.709964012595591, 13.642565495143106, 2.1155696702443207], "isController": false}, {"data": ["C1 GET /produtos?nome=", 2287, 0, 0.0, 40.00655881066902, 5, 583, 15.0, 99.20000000000027, 178.5999999999999, 390.1199999999999, 38.67618210106203, 14.239180324316782, 7.402862980281404], "isController": false}, {"data": ["C1 GET /produtos/{id}", 2310, 0, 0.0, 36.410389610389586, 5, 518, 13.0, 83.90000000000009, 161.0, 393.0199999999977, 38.763592428513896, 14.385303789728487, 7.078898226691503], "isController": false}, {"data": ["C3 GET /vendas (lista completa)", 65, 0, 0.0, 14386.600000000002, 7357, 24876, 13328.0, 23384.0, 24271.4, 24876.0, 0.8429516275450655, 553.4283663354299, 0.15064467562572947], "isController": false}, {"data": ["C2 GET /vendas/{id}", 1630, 0, 0.0, 32.27423312883431, 10, 390, 18.0, 66.0, 99.0, 226.76000000000022, 27.4655837700305, 15.719172699336108, 5.031566859950797], "isController": false}, {"data": ["C2 POST /vendas", 2698, 1060, 39.288361749444036, 66.3884358784283, 17, 445, 45.0, 138.0, 197.04999999999973, 305.00999999999976, 45.059037693939246, 22.786588809142074, 13.24489291589425], "isController": false}, {"data": ["C3 POST /vendas", 698, 86, 12.320916905444125, 4498.213467048713, 17, 15734, 4400.0, 7739.4, 13502.949999999999, 15465.13, 9.888366294554316, 5.539060618483312, 2.9066389205672354], "isController": false}, {"data": ["C1 GET /produtos/{id}/estoque", 2301, 0, 0.0, 36.45762711864403, 5, 501, 13.0, 86.0, 160.80000000000018, 372.9200000000001, 38.700888051668464, 13.291555020729614, 7.369798017651709], "isController": false}, {"data": ["C3 GET /produtos/{id}/estoque", 778, 0, 0.0, 4735.12467866324, 6, 15610, 4308.0, 11567.3, 13611.499999999995, 15466.21, 10.94911056068453, 3.6782168289799597, 2.085035702474105], "isController": false}, {"data": ["setUp POST /clientes", 1, 0, 0.0, 379.0, 379, 379, 379.0, 379.0, 379.0, 379.0, 2.638522427440633, 1.1517768799472294, 0.8245382585751979], "isController": false}, {"data": ["C1 GET /produtos", 2317, 0, 0.0, 37.755718601640126, 5, 527, 14.0, 89.20000000000027, 173.0, 344.0200000000018, 38.79902207039754, 45.278155638794, 7.009588948265179], "isController": false}, {"data": ["tearDown GET estoque produto A", 1, 1, 100.0, 14.0, 14, 14, 14.0, 14.0, 14.0, 14.0, 71.42857142857143, 23.995535714285715, 13.602120535714285], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["Lost update: Vendas aprovadas (201): 2250 | estoque esperado: 997750 | estoque no banco: 998606.000 | baixas perdidas: 856.000", 1, 0.08718395815170009, 0.0054935999560512], "isController": false}, {"data": ["500", 1146, 99.9128160418483, 6.295665549634675], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 18203, 1147, "500", 1146, "Lost update: Vendas aprovadas (201): 2250 | estoque esperado: 997750 | estoque no banco: 998606.000 | baixas perdidas: 856.000", 1, "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["C2 POST /vendas", 2698, 1060, "500", 1060, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["C3 POST /vendas", 698, 86, "500", 86, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["tearDown GET estoque produto A", 1, 1, "Lost update: Vendas aprovadas (201): 2250 | estoque esperado: 997750 | estoque no banco: 998606.000 | baixas perdidas: 856.000", 1, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
