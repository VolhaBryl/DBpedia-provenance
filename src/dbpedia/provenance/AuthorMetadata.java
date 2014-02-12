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

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Extracts author metadata using Mediawiki API: under developments
 * See resources/scripts folder for the working Perl version
 *
 * @author VolhaBryl
 *
 */
public class AuthorMetadata
{
    public Integer WikiUserEditCount(String sUser)
    {
        Integer cnt = -1;
        String url = String.format("http://en.wikipedia.org/w/api.php?action=query&list=users&ususers=" + sUser + "&usprop=editcount&format=xml");
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);

        try
        {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = httpclient.execute(httppost, responseHandler);

            String pattern = "editcount=\"(.*)\"";
            Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher m = r.matcher(response);
            if (m.find( ))
            {
                cnt = Integer.valueOf(m.group(1));
                // System.out.println("number of edits, user " + sUser + ": " + m.group(1) );
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }

        return cnt;
    }
}
