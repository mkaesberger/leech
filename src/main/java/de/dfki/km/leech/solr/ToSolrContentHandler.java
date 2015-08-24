package de.dfki.km.leech.solr;



import java.rmi.server.UID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.metadata.Metadata;

import de.dfki.inquisition.collections.MultiValueHashMap;
import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.sax.DataSinkContentHandler;



public class ToSolrContentHandler extends DataSinkContentHandler
{

    public static void main(String[] args)
    {

    }

    protected MultiValueHashMap<String, String> m_hsStaticAttValuePairs = new MultiValueHashMap<String, String>();



    protected SolrClient m_solrClient;



    protected String m_strSolrUrl;


    public ToSolrContentHandler(String solrUrl)
    {
        this.m_strSolrUrl = solrUrl;

        // hier besser einen ConcurrentUpdateSolrClient nehmen, der soll beim Indexieren besser performen
        int iCores = Runtime.getRuntime().availableProcessors();
        m_solrClient = new ConcurrentUpdateSolrClient(m_strSolrUrl, 2056, iCores / 2);
    }



    @Override
    public void crawlFinished()
    {
        try
        {
            m_solrClient.commit();
            m_solrClient.close();
        }
        catch (Exception e)
        {
            Logger.getLogger(ToSolrContentHandler.class.getName()).log(Level.SEVERE, "Error", e);
        }
    }





    /**
     * Sets some attribute value pairs that will be added to every crawled document.
     * 
     * @return the current static attribute value pairs
     */
    public MultiValueHashMap<String, String> getStaticAttributeValuePairs()
    {
        return m_hsStaticAttValuePairs;
    }



    @Override
    public void processErrorData(Metadata metadata)
    {
        // NOP
    }



    @Override
    public void processModifiedData(Metadata metadata, String strFulltext)
    {

        // sadly, there is no update method

        this.processRemovedData(metadata);

        this.processNewData(metadata, strFulltext);

    }



    @Override
    public void processNewData(Metadata metadata, String strFulltext)
    {

        try
        {
            SolrInputDocument doc = new SolrInputDocument();

            if(metadata.getValues(LeechMetadata.id).length == 0) doc.addField(LeechMetadata.id, new UID().toString());
            doc.addField(LeechMetadata.body, strFulltext);

            for (String strFieldName : metadata.names())
            {
                for (String strFieldValue : metadata.getValues(strFieldName))
                {
                    doc.addField(strFieldName, strFieldValue);
                }
            }
            
            //die statischen AttValue Paare
            MultiValueHashMap<String,String> mhsStaticAttributeValuePairs = getStaticAttributeValuePairs();
            
            for(Entry<String, String> att2value : mhsStaticAttributeValuePairs.entryList())
                doc.addField(att2value.getKey(), att2value.getValue());


            m_solrClient.add(doc);


        }
        catch (Exception e)
        {
            Logger.getLogger(ToSolrContentHandler.class.getName()).log(Level.SEVERE, "Error", e);
        }

    }



    @Override
    public void processProcessedData(Metadata metadata)
    {
        // NOP
    }



    @Override
    public void processRemovedData(Metadata metadata)
    {

        try
        {
            m_solrClient.deleteById(metadata.get(IncrementalCrawlingHistory.dataEntityId));
        }
        catch (Exception e)
        {
            Logger.getLogger(ToSolrContentHandler.class.getName()).log(Level.SEVERE, "Error", e);
        }

    }



    @Override
    public void processUnmodifiedData(Metadata metadata)
    {
        // NOP
    }



    /**
     * Sets some attribute value pairs that will be added to every crawled document.
     * 
     * @param hsStaticAttValuePairs a multi value map containing the additional attribute value pairs
     * 
     * @return this
     */
    public ToSolrContentHandler setStaticAttributeValuePairs(MultiValueHashMap<String, String> hsStaticAttValuePairs)
    {
        m_hsStaticAttValuePairs = hsStaticAttValuePairs;

        return this;
    }

}
