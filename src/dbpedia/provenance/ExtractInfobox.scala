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

package dbpedia.provenance

import java.io._
import java.net.URL
import org.dbpedia.extraction.destinations.formatters.{TerseFormatter, TriXFormatter, TripleBuilder}
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{Source, WikiSource, XMLSource, WikiPage}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, WikiParser, WikiTitle}

/* Input: Infobox string, output: RDF triples
*  uses the  DBpedia Extraction Framework - https://github.com/dbpedia/extraction-framework
*
* @author VolhaBryl
*
*/
class ExtractInfobox(language : Language)
{
  val lang = language
  val loaded_redirects = loadRedirects(lang)
  val loaded_mappings = loadMappings(lang)

  def loadRedirects(lang : Language) : Redirects =
  {
    // val file = new File("C:\\wikidumps\\", "enwiki-20130503-pages-articles.xml")
    val file = new File("redirects", "pages-articles.xml") // no such file
    val source = XMLSource.fromFile(file,lang)
    //
    // from file or source:
    // val cache = new File("C:\\wikidumps\\", "enwiki-20130403-template-redirects.obj")
    val cache = new File("redirects", lang.isoCode+"-template-redirects.obj") // TEST
    val rd = Redirects.load(source,cache,lang)

    /*
    // from file + save file to cache:
    // (old version)
    val rd = Redirects.loadFromSource(source, lang)
    val cache = new File("C:\\wikidumps\\", "template-redirects.obj")
    val dir = cache.getParentFile
    val outputStream = new ObjectOutputStream(new FileOutputStream(cache))
    try
    {
      outputStream.writeObject(rd.map)
    }
    finally
    {
      outputStream.close()
    }*/

    rd
  }

  def loadMappings(lang : Language) : Mappings =
  {
    // TODO: language in file names!

    val parser = WikiParser.getInstance()

    /*  [working, loading from the wiki]
    // load the ontology:
    val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
    val url = new URL(Language.Mappings.apiUri)
    val ontologyPages = WikiSource.fromNamespaces(namespaces, url, Language.Mappings)
    val loaded_ontology = new OntologyReader().read(ontologyPages)

    // load mappings:
    val namespace = Namespace.mappings(lang)
    val namespaces_2 = Set(namespace)
    val mappingsPages = WikiSource.fromNamespaces(namespaces_2,url,Language.Mappings).map(parser)
    */

    // load from files:
    // load the ontology:
    val file_ont = new File("mappings", "ontology.xml")
    val ontologySource = XMLSource.fromFile(file_ont, Language.Mappings)
    val loaded_ontology = new OntologyReader().read(ontologySource)

    // load mappings:
    val file_map = new File("mappings", "Mapping_"+lang.isoCode+".xml")
    val mappingsPages = XMLSource.fromFile(file_map, Language.Mappings).map(parser)

    val context = new {
      val ontology = loaded_ontology
      val language = lang
      val redirects: Redirects = loaded_redirects
      val mappingPageSource = mappingsPages
    }
    MappingsLoader.load(context)
  }

  // def extract(title : String, content : String) : String  =
  def extract(title : String, content : String) : Seq[org.dbpedia.extraction.destinations.Quad]  =
  {
    // Source phase skipped, a new page constructed:
    //// val wt = new WikiTitle(title, Namespace.Main, Language.English,false, null)
    val page = new WikiPage(WikiTitle.parse(title, lang), content)

    // Parser:
    val parser = WikiParser.getInstance()
    val ast = parser(page)

    // Extractor
    val pc = new PageContext()
    val uri = page.title.language.resourceUri.append(page.title.decodedWithNamespace)

    // without redirects:
    //// val extractor = new MappingExtractor(new {val mappings = loaded_mappings; val redirects = new Redirects(Map())})
    // with redirects:
    val extractor = new MappingExtractor(new {val mappings = loaded_mappings; val redirects = loaded_redirects})
    // other extractor (raw infobox properties), just testing:
    //// val extractor = new InfoboxExtractor(new {val ontology = loaded_ontology; val language = lang; val redirects = new Redirects(Map())})

    // val triples = extractor.extract(ast, "", new PageContext()) -->
    val triples = extractor.extract(ast, uri, pc)

    /*for (q <- triples)
    {
      println("subject " + q.subject + "; predicate " + q.predicate + "; value " + q.value)
    }
    println("number of triples: "+triples.size)
    */

    /*
    // Destination (write to string buffer):
    val writer = new StringWriter
    val destination = new WriterDestination(() => writer, new TerseFormatter(true, false, null))
    destination.open()
    destination.write(triples)
    destination.close()
    val out = writer.toString
    println(out)
    out
    // Write to file:
    // val writer = new FileWriter
    // val file = new File("mappings", "myoutput")
    // val destination = new WriterDestination(writer(file),  new TerseFormatter(false, true))
    // destination.write(triples)
    // destination.close()
    */

    triples
  }
}
