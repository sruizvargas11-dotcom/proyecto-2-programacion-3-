package una.eif206.data;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class XMLHelper {

  private String path;
  private static XMLHelper theInstance;

  public static XMLHelper instance() {
    if (theInstance == null) theInstance = new XMLHelper("data.xml");
    return theInstance;
  }

  public XMLHelper(String p) {
    path = p;
  }

  public Data load() throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance(Data.class);
    try (FileInputStream is = new FileInputStream(path)) {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      return (Data) unmarshaller.unmarshal(is);
    }
  }

  public void store(Data d) throws Exception {
    File destino = new File(path);
    File temporal = new File(path + ".tmp");
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Data.class);
      try (FileOutputStream os = new FileOutputStream(temporal)) {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(d, os);
        os.flush();
      }
      Files.move(temporal.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      if (temporal.exists()) {
        temporal.delete();
      }
      throw e;
    }
  }
}