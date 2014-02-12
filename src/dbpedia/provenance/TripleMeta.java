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

/* Store metadata about a DBpedia triple
*
* @author VolhaBryl
*
* */
public class TripleMeta
{
    // private String subject;
    private String predicate;
    private String value;
    private String lastEdit;
    private String lastAuthor;
    private int numIter = 0;

    public TripleMeta(String _predicate, String _value, String _lastEdit, String _lastAuthor)
    {
        predicate = _predicate;
        value = _value;
        lastEdit = _lastEdit;
        lastAuthor = _lastAuthor;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(String lastEdit) {
        this.lastEdit = lastEdit;
    }

    public String getLastAuthor() {
        return lastAuthor;
    }

    public void setLastAuthor(String lastAuthor) {
        this.lastAuthor = lastAuthor;
    }

    public int getNumIter() {
        return numIter;
    }

    public void setNumIter(int numIter) {
        this.numIter = numIter;
    }

    public void updateToCurrentIteration(String lastEdit, String lastAuthor, int numIter) // if stored iteration == previous iteration
    {
        if (this.numIter == numIter - 1)
        {
            this.lastEdit = lastEdit;
            this.lastAuthor = lastAuthor;
            this.numIter = numIter;
        }
    }
}
