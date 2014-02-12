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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;
import java.util.SortedSet;

import scala.Option;
import scala.collection.Seq;
import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.extraction.util.Language;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/* Parses XML Wikipedia revision dumps - http://dumps.wikimedia.org/ -
*  to extract provenance metadata (last edit date and author, number of edits) and edit traces
*  for DBpedia facts.
*  Tuned to be used for 3.9 version, and for PopulatedPlace classe (can be changed in the code accordingly).
*  Takes as input DBpedia ontology and mappings - mappings.dbpedia.org (see mappings folder).
*
*  See examples of the output in resources/output
*
*  Note 1: this is a first version of the tool, the work on it is ongoing.
*
*  Note 2: processing all dumps takes LONG on a normal PC, where LONG means weeks for English DBpedia and days for smaller editions.
*
* @author VolhaBryl
*
* */
public class WikiRevSAX extends DefaultHandler
{
	private static String LastRelease = "2013-04-04T00:00:00Z"; // date for the dumps used for DBpedia 3.9

	/* variables */
	private String currTag;
	private String sPage;

	private String sAuthorIP;
	private String sAuthorID;
	private String sAuthorName;
    private String sTimestamp;
    private String sText;
    private String sNS;

    // used to collect all text fields
    private StringBuilder currentText;

    private boolean inRevision;
    private boolean NSZerro;

    // Type filtering:
    private boolean typeFilter;
    private String filterPred = "type";
    private String filterValue = "PopulatedPlace";

    private HashMap<String, String> revisions;
	private HashMap<String, String> authors;

    // Output files
    private PrintWriter out;
    private PrintWriter tr;
    private PrintWriter log;
    private String sInputFile;
    // DBpedia Infobox Extrator
    ExtractInfobox e;

    private int nDebug;

    /* Constructor */
    public WikiRevSAX (Language lang, String fn, String outdir)
    {
    	super();
        // TODO: remove
        nDebug = 0;

        sInputFile = fn;

        e = new ExtractInfobox(lang);
        try
        {
            String name = new File(fn).getName();

            out = new PrintWriter(outdir+name+".out", "UTF-8");
            log = new PrintWriter(outdir+name+".log", "UTF-8");
            tr = new PrintWriter(outdir+name+".trace", "UTF-8");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    public void close()
    {
        out.close();
        log.close();
        tr.close();
    }

    /* ============================= */
    /* Start and end document events */
    /* ============================= */
    public void startDocument () throws SAXException
    {
    	inRevision = false;

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        log.println("start document: " + sdf.format(cal.getTime()));
        System.out.println("start document: " + sdf.format(cal.getTime()));
    }

    public void endDocument () throws SAXException
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        log.println("end: " + sdf.format(cal.getTime()));
        System.out.println("end: " + sdf.format(cal.getTime()));

        // TODO: delete input file here!
    }
 
    /* ============================================================= */
    /* ============================================================= */
    
    /* Start element event */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException 
	{
		currTag = qName;
		
		if (qName.equalsIgnoreCase("page")) 
		{
			revisions = new HashMap<String, String>();
			authors = new HashMap<String, String>();
            NSZerro = true;
            typeFilter = true;
		}
		if (qName.equalsIgnoreCase("revision")) 
		{
			inRevision = true;
		}
		if (qName.equalsIgnoreCase("text") || qName.equalsIgnoreCase("title") || qName.equalsIgnoreCase("timestamp") || qName.equalsIgnoreCase("ns") ||
                qName.equalsIgnoreCase("username") || qName.equalsIgnoreCase("id") || qName.equalsIgnoreCase("ip"))
        {
            // inText = true;
            currentText = new StringBuilder("");
        }

    }

    private void printExceptionInfo(Exception e)
    {
        System.err.println(e.getMessage());
        e.printStackTrace();
        System.out.println("EXCEPTION_AT \"" + sPage + "\" \"" +  sInputFile + "\"");
        System.out.println(e.getMessage());
        // print stack trace:
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.out.println(sw.toString());
    }

	/* End element event */
	public void endElement(String uri, String localName, String qName) throws SAXException 
	{ 
		currTag = "";

        if (qName.equalsIgnoreCase("page"))
		{
            processPage();
            /*try
            {
                processPage();
            }
            catch (Exception e)
            {
                printExceptionInfo(e);
            }*/
		}
		if (qName.equalsIgnoreCase("revision"))
		{
			// if timestamp greater than "dbpedia extraction date" then stop saving revisions:			
			if (sTimestamp.compareTo(LastRelease) <= 0 && NSZerro)
			{
				revisions.put(sTimestamp, sText);
				if (sAuthorIP == null || sAuthorIP.isEmpty())
				{
					authors.put(sTimestamp, sAuthorID + "\t" + sAuthorName);
				}
				else
				{
					authors.put(sTimestamp, sAuthorIP + "\t");
				}
			}
			inRevision = false;
		}
		if (qName.equalsIgnoreCase("text"))
		{
			sText =  currentText.toString();
			// inText = false;
		}
        if (qName.equalsIgnoreCase("timestamp"))
        {
            sTimestamp =  currentText.toString();
        }
        if (qName.equalsIgnoreCase("title"))
        {
            sPage =  currentText.toString();
        }
        if (qName.equalsIgnoreCase("ns"))
        {
            sNS =  currentText.toString();
            if (!sNS.equals("0")) NSZerro = false;
        }
		if (inRevision)
		{
			if (qName.equalsIgnoreCase("ip"))
			{
				sAuthorIP = currentText.toString();
                sAuthorName = "";
                sAuthorID = "";
			}
			if (qName.equalsIgnoreCase("id"))
			{
				sAuthorID = currentText.toString();
                sAuthorIP = "";
            }
			if (qName.equalsIgnoreCase("username"))
			{
				sAuthorName = currentText.toString();
                sAuthorIP = "";
                if (sAuthorName.contains("\t"))
                {
                    log.print(sAuthorName);
                    sAuthorName.replace("\t'"," ");
                    log.print(" --- " + sAuthorName + "\n");
                    System.out.println(sAuthorName);
                }
            }
		}
	}
	
	/* Process element text content event */
	public void characters(char ch[], int start, int length) throws SAXException 
	{

        if (currTag.equalsIgnoreCase("text") || currTag.equalsIgnoreCase("title") || currTag.equalsIgnoreCase("timestamp") || currTag.equalsIgnoreCase("ns") ||
                currTag.equalsIgnoreCase("username") || currTag.equalsIgnoreCase("id") || currTag.equalsIgnoreCase("ip"))
        {
            currentText = currentText.append(ch, start, length);
        }
    }

    /* Process the sequence of revisions */
    public void processPage() throws SAXException
    {
        if (!sNS.equals("0")) return; // process only the main namespace

        /*
        if (nDebug > 30)
        {
            System.out.println("END OF PROCESSING");
            throw new SAXException();
        }
        else nDebug++;
        */

        if (nDebug%100 == 0)
        {
            System.out.println(nDebug + ": " + sPage);
        }
        nDebug++;

        // processing revisions content:
        List<String> blocks = new ArrayList<String>();
        List<String> current_blocks = new ArrayList<String>();
        List<TripleMeta> triples = new ArrayList<TripleMeta>();
        int iteration_num = 0;
        String currSubject = null;
        // for edit counts:
        List<TripleMeta> prev_triples = new ArrayList<TripleMeta>();
        String prevTimeStamp = null;
        List<TripleMeta> curr_triples = new ArrayList<TripleMeta>();
        HashMap<String,Integer> property_edits = new HashMap<String,Integer>();
        HashMap<String,String> property_trace = new HashMap<String,String>();
        //
        SortedSet<String> keys = new TreeSet<String>(revisions.keySet()).descendingSet(); // (reverse the order wrt timestamp)
        for (String timeStamp : keys)
        {
            if (!typeFilter) break;

            String s = revisions.get(timeStamp);
            if (s.length() == 0) break;
            boolean to_process = false;

            // Extract {{..}}-blocks:
            int num_blocks = 0;
            char prev = revisions.get(timeStamp).charAt(0);
            int last_str_start = -1;
            int opened_pars = 0;
            current_blocks.clear();
            for (int i = 1; i < s.length(); i++)
            {
                char c = s.charAt(i);
                if (c == '{' )
                {
                    if (prev == '{') // double {{
                    {
                        if (opened_pars == 0) last_str_start = i+1; // new 1st-level block starts
                        opened_pars += 2;
                    }
                    else
                    {
                        if (opened_pars > 0) opened_pars++;
                    }
                }
                else if (opened_pars > 0)   // inside the block
                {
                    if ((c == '}') && (prev == '}')) // double }}
                    {
                        opened_pars -= 2;
                        if (opened_pars == 0 && (last_str_start <= i - 2)) // end of the (non-empty) block
                        {
                            num_blocks++;
                            String sub = s.substring(last_str_start, i - 2);
                            current_blocks.add(sub); // save last blocks

                            if (iteration_num == 0) // just add new blocks
                            {
                                blocks.add(sub);
                            }
                            else
                            {
                                if (!blocks.contains(sub)) to_process = true;
                            }
                            opened_pars = 0;
                            last_str_start = -1;
                        }
                    }
                    if ((c != '}') && (prev == '}')) // single }
                    {
                        opened_pars--; // can become 0
                    }
                }
                prev = c;
            }

            int curr_size = blocks.size();
            blocks.clear();
            blocks.addAll(current_blocks);

            if (num_blocks != curr_size) to_process = true;   // if there are more (or less) blocks than in the current DBpedia version (according to LastRelease timestamp) => infobox may have changed, so processing needed
            if ((num_blocks == 0) && (curr_size == 0)) break; // if current and previous page editions contain no {{}} blocks => no infobox => no need to further process it

            if (to_process || (iteration_num == 0)) // only in this case call the extractor
            {
                if (num_blocks != 0)  //  num_blocks == 0 and curr_size != 0 means infobox was introduced at prevTimeStamp
                {
                    try
                    {
                        if (iteration_num == 0) typeFilter = false;
                        Seq<Quad> out = (Seq<Quad>) e.extract(sPage,revisions.get(timeStamp));
                        scala.collection.Iterator <Quad> iter = (scala.collection.Iterator<Quad>) out.iterator();
                        while (iter.hasNext()) // iterating over extracted triples
                        {
                            Quad q = iter.next();
                            if (iteration_num == 0)
                            {
                                if (currSubject == null)
                                {
                                    currSubject = q.subject();
                                }
                                TripleMeta tm = new TripleMeta(q.predicate(),q.value(),timeStamp,authors.get(timeStamp));
                                triples.add(tm);
                                property_edits.put(q.predicate(), 0);
                                property_trace.put(q.predicate(), "");

                                typeFilter = ApplyTypeFilter(q.predicate(),q.value());
                            }
                            else
                            {
                                // if property-value hasn't changed between revisions, decrease author/timestamp for yet unprocessed predicate-value pairs:
                                for (int i = 0; i < triples.size(); i++)
                                {
                                    if (triples.get(i).getPredicate().equals(q.predicate()) && triples.get(i).getValue().equals(q.value()))
                                    {
                                        triples.get(i).updateToCurrentIteration(timeStamp, authors.get(timeStamp), iteration_num);
                                    }
                                }
                            }
                            // [number of edits] save current step:
                            curr_triples.add(new TripleMeta(q.predicate(),q.value(),timeStamp,authors.get(timeStamp)));
                        }
                    }
                    catch (Exception e)
                    {
                        printExceptionInfo(e);
                    }
                }
                // [number of edits] compare with previous (later in time) step (1st scan)
                List<String> found_props = new ArrayList<String>();
                for (int i = 0; iteration_num > 0 && i < curr_triples.size(); i++)
                {
                    String cp = curr_triples.get(i).getPredicate();
                    if (property_edits.containsKey(cp))
                    {
                        String cv = curr_triples.get(i).getValue();
                        boolean found = false;
                        for (int j = 0; j < prev_triples.size(); j++)
                        {
                            String pp = prev_triples.get(j).getPredicate();
                            String pv = prev_triples.get(j).getValue();
                            if (cp.equals(pp) && cv.equals(pv))
                            {
                                found = true;
                                prev_triples.get(j).setNumIter(iteration_num); // mark found prev_triples
                            }
                        }
                        // [number of edits] if no triple found, add property to be counted
                        if (!found && !found_props.contains(cp)) found_props.add(cp);
                    }
                }
                // [number of edits] if a triple was deleted, add property to be counted (the 2nd scan)
                for (int j = 0; j < prev_triples.size(); j++)
                {
                    if (prev_triples.get(j).getNumIter() != iteration_num)
                    {
                        String pp = prev_triples.get(j).getPredicate();
                        if (property_edits.containsKey(pp) && !found_props.contains(pp)) found_props.add(pp);
                    }
                }
                // [number of edits] increase counts
                for (String p : found_props)
                {
                    property_edits.put(p, property_edits.get(p) + 1);
                    String pv = SaveValueTrace(prev_triples, p);
                    property_trace.put(p, property_trace.get(p) + "\t" + prevTimeStamp + "\t" + authors.get(prevTimeStamp) + pv + ";\n");
               }
                // [number of edits] save previous step:
                prev_triples.clear();
                prev_triples.addAll(curr_triples);
                curr_triples.clear();
            }
            else
            {
                // if nothing has changed, decrease author/timestamp for yet unprocessed predicate-value pairs:
                for (int i = 0; i < triples.size(); i++)
                {
                    triples.get(i).updateToCurrentIteration(timeStamp, authors.get(timeStamp), iteration_num);
                }
            }
            iteration_num++; // next time not just record blocks, but compare
            prevTimeStamp = timeStamp;

            if ((iteration_num == 1) && triples.size() == 0) break; // if no triples were extracted during the 1st iteration => no infobox, no need to continue
        } // [end of the for-cycle by timestamp]
        String sEarliestTimestamp = keys.last();

        // triples + metdata, output:
        if (typeFilter)
        {
            for (int i = 0; i < triples.size(); i++)
            {
                String ss = currSubject + "\t" + triples.get(i).getPredicate() + "\t" + triples.get(i).getValue() + "\t" + triples.get(i).getLastEdit() + "\t" + triples.get(i).getLastAuthor();
                // System.out.println(ss);
                out.println(ss);
            }
            // [number of edits] number of edits, output:
            String lts = "";
            if (sEarliestTimestamp.equals(prevTimeStamp) && prev_triples.size() > 0) // infobox was present at the earliest edition of a page
            {
                lts = "\t" + sEarliestTimestamp + "\t" + authors.get(sEarliestTimestamp);
            }

            for (Map.Entry<String, Integer> e : property_edits.entrySet())
            {
                Integer count =  e.getValue();
                String lts_v = "";
                if (!lts.equals(""))
                {
                    lts_v = SaveValueTrace(prev_triples, e.getKey()) + ";\n";
                    count++;
                }
                tr.print(currSubject + "\t" + e.getKey() + "\t" + count + "\n" + property_trace.get(e.getKey()) + lts + lts_v);
            }
        }
        // end of processing

        revisions.clear();
        authors.clear();
    }

    private boolean ApplyTypeFilter(String pred, String value)
    {
        if (typeFilter) return true;
        if (pred.contains(filterPred) && value.contains(filterValue)) return true;
        return false;
    }

    /* Save all values per property p from trp*/
    private String SaveValueTrace(List<TripleMeta> trp, String p)
    {
        String pv = "";
        for (int j = 0; j < trp.size(); j++)
        {
            if (trp.get(j).getPredicate().equals(p))
            {
                pv = pv + "\t" + trp.get(j).getValue();
            }
        }
        if (pv.isEmpty()) pv = "\t";

        return pv;
    }

    /* ================ */
	/* ===== main ===== */
	/* ================ */
    public static void main (String args[])	throws Exception
    {
        /*
        // Testing per page revision history loading
        LoadPageRevisions lrev = new LoadPageRevisions();
        lrev.loadByExport("en", "Ladenburg", "resources\\export");
        System.out.println("iterations: " + lrev.loadByQuery("en", "Ladenburg", "\\out\\query"));

        String outdir = "resources\\export\\";
        String fn = "resources\\export\\en-Ladenburg.xml";
        Option<Language> lang = Language.get("en");

        dbpedia.provenance.WikiRevSAX h = new dbpedia.provenance.WikiRevSAX(lang.get(), fn, outdir);
        SAXParserFactory pF = SAXParserFactory.newInstance();
        pF.setValidating(false);

        SAXParser p = pF.newSAXParser();
        try
        {
            p.parse(new File(fn), h);
        }
        catch (Exception e)
        {
            h.printExceptionInfo(e);
        }
        h.close();
        */

        /*
        // Testion user metadata loading
        String sUser = "Leahtwosaints";
        AuthorMetadata am = new AuthorMetadata();
        System.out.println("number of edits, user " + sUser + ": " + am.WikiUserEditCount(sUser) );
        */

        // Provenance metadata extraction
        if (args.length != 3)
        {
            System.out.println("wrong arguments");
            return;
        }
        String fn = args[0];
        String outdir = args[1];
        Option<Language> lang = Language.get(args[2]);
        System.out.println(fn);
        System.out.println(lang.get());

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println("start: " + sdf.format(cal.getTime()));

        // String outdir = "C:\\wikidumps\\";
    	// String fn = "C:\\wikidumps\\enwiki-latest-pages-meta-history11.xml";
        // String fn = "C:\\wikidumps\\en\\enwiki-latest-pages-meta-history1.xml-p000000010p000002594";
        // Option<Language> lang = Language.get("en");

        dbpedia.provenance.WikiRevSAX h = new dbpedia.provenance.WikiRevSAX(lang.get(), fn, outdir);

    	SAXParserFactory pF = SAXParserFactory.newInstance();
    	pF.setValidating(false); 

    	SAXParser p = pF.newSAXParser();
        try
        {
            p.parse(new File(fn), h);
        }
        catch (Exception e)
        {
            h.printExceptionInfo(e);
        }
        h.close();
     }

}

