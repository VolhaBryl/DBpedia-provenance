/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dbpedia.provenance;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/* Loads revision history of a single page via mediaWiki API: is slow for popular frequently edited pages!
*  (e.g. loading the revision history for Albert Einstein took >30 minutes and yielded >1GB dump)
*
* @author VolhaBryl
*
* */
public class LoadPageRevisions
{
    private String startPage = "<page ";
    private String endPage = "</page>";
    private String startRev = "<revisions>";
    private String endRev = "</revisions>";
    private String continueStart = "<continue ";
    private String continueEnd = " />";

    // The format of the output is compatible with the format WikiRevSAX uses
    public boolean loadByExport(String lang, String page, String dir)
    {
        try
        {
            FileWriter fstream = new FileWriter(dir+lang+"-"+page+".xml");
            BufferedWriter out = new BufferedWriter(fstream);

            URL url = new URL("http://"+lang+".wikipedia.org/w/index.php?title=Special:Export&pages="+page+"&history&action=submit");
            URLConnection connection = url.openConnection();
            connection.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            StringBuilder builder = new StringBuilder();
            while((inputLine = in.readLine()) != null)
            {
                builder.append(inputLine+"\n");
            }
            String content = builder.toString();
            out.write(content);
            in.close();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    // The format of the output is NOT compatible with the format WikiRevSAX uses
    // TODO: change WikiRevSAX to work also with this format
    public int loadByQuery(String lang, String page, String dir)
    {
        int reqNum = 0;

        try
        {
            String reqContinue = "continue=";

            String req1 = "https://";
            String req2 = ".wikipedia.org/w/api.php?action=query&prop=revisions&titles=";
            // String req3 = "&rvlimit=max&rvprop=timestamp|user&format=xml&";
            String req3 = "&rvlimit=max&rvprop=timestamp|user|content&format=xml&";

            FileWriter fstream = new FileWriter(dir+lang+"-"+page+".xml");
            BufferedWriter out = new BufferedWriter(fstream);

            boolean go = true;
            boolean first = true;
            while (go)
            {
                URL url = new URL(req1 + lang + req2 + page + req3 + reqContinue);
                URLConnection connection = url.openConnection();
                connection.connect();

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                // line 1:
                String inputLine = in.readLine();
                String lineToProcess = null;
                if (first) // include page info in the output
                {
                   first = false;
                   lineToProcess = inputLine.substring(inputLine.indexOf(startPage),inputLine.length()-1);
                }
                else // start from the next <rev> element
                    lineToProcess = inputLine.substring(inputLine.indexOf(startRev)+startRev.length(),inputLine.length()-1);

                lineToProcess = lineToProcess.replaceAll(">",">\n");
                // lines 2 - last-1
                StringBuilder builder = new StringBuilder();
                while((inputLine = in.readLine()) != null)
                {
                    builder.append(lineToProcess+"\n");
                    lineToProcess = inputLine;
                    // opt1: System.out.println(inputLine);
                    // opt2: out.write(inputLine+"\n");
                }
                // last line:
                builder.append(lineToProcess.substring(0,lineToProcess.indexOf(endRev)));
                // need to continue?
                int ind = lineToProcess.indexOf(continueStart);
                if (ind != -1) // yes - prepare new request
                {
                    int indEnd = lineToProcess.indexOf("/>",ind);
                    reqContinue = lineToProcess.substring(ind+continueStart.length(),indEnd-1);
                    reqContinue = reqContinue.replaceAll("\"","");
                    reqContinue = reqContinue.replaceAll(" ","&");
                    System.out.println("continue: "+reqContinue);
                }
                else // no - close the page tag, end cycle
                {
                    builder.append(endRev+"\n"+endPage);
                    go = false;
                }
                String content = builder.toString();
                out.write(content);
                in.close();
                reqNum++;
            }
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

       return reqNum;
    }
}