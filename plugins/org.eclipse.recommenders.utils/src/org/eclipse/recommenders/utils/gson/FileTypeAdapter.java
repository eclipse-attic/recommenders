package org.eclipse.recommenders.utils.gson;

import java.io.File;
import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class FileTypeAdapter extends TypeAdapter<File> {

    @Override
    public void write(JsonWriter out, File value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public File read(JsonReader in) throws IOException {
        return new File(in.nextString());
    }

}
