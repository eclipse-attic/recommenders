package org.eclipse.recommenders.models;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.recommenders.utils.Constants;

public class ModelIndexTestUtils {

    public static Directory inMemoryIndex(Document... documents) throws Exception {
        RAMDirectory directory = new RAMDirectory();
        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, new KeywordAnalyzer());
        IndexWriter writer = new IndexWriter(directory, conf);
        for (Document document : documents) {
            writer.addDocument(document);
        }
        writer.close();
        return directory;
    }

    public static Document coordinateWithSymbolicName(Artifact coordinate, String symbolicName) {
        Document doc = new Document();
        doc.add(newStored(Constants.F_COORDINATE, coordinate.toString()));
        doc.add(newStored(Constants.F_SYMBOLIC_NAMES, symbolicName));
        return doc;
    }

    private static Field newStored(String key, String value) {
        return new Field(key, value, Store.YES, Index.NOT_ANALYZED);
    }
}
