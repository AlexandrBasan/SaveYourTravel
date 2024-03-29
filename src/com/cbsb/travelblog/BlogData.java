package com.cbsb.travelblog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import com.cbsb.travelblog.R;

public class BlogData
{
   private List<BlogElement> mBlogs = new ArrayList<BlogElement>();
   private String mFile = null;
   private static final String mPath = TravelLocBlogMain.TRIP_PATH;

   /*
    * this function closes anything that is already open, then opens new file,
    * or creates
    */
   public Boolean openBlog(String filename)
   {
      return privOpenFile(filename);
   }

   private Boolean privOpenFile(String filename)
   {
      if (filename != null)
      {
         // Log.d("TRAVEL_DEBUG", "Opening file: "+ filename);
         File newFile = new File(Environment.getExternalStorageDirectory()
               + mPath);
         try
         {
            newFile.mkdirs();
         }
         catch (Exception e)
         {

         }
      }

      mFile = filename;
      mBlogs.clear();
      if (filename != null)
      {
         return openBlogFromFile();
      }
      return false;
   }

   /* for a new blog, we simply open it, and save it */
   public Boolean newBlog(String filename)
   {
      privOpenFile(filename);
      return saveBlogToFile();
   }

   /* updates element if index valid, otherwise creates new element if index -1 */
   public Boolean saveBlogElement(BlogElement blog, int index)
   {
      if ((blog.location == null) || (blog.location.length() == 0)
            || (blog.name == null) || (blog.name.length() == 0))
      {
         return false;
      }
      String str = blog.name.trim();
      blog.name = str;
      str = blog.description.trim();
      blog.description = str;
      str = blog.location.trim();
      blog.location = str;

      if (index == -1)
      {
         mBlogs.add(blog);
         if (saveBlogToFile() == false)
         {
            refreshData();
            return false;
         }
         return true;
      }
      else if (index < mBlogs.size())
      {
         mBlogs.set(index, blog);
         if (saveBlogToFile() == false)
         {
            refreshData();
            return false;
         }
         return true;
      }
      return false;
   }

   /* deletes blog element */
   public Boolean deleteBlogElement(int index)
   {
      if (index < mBlogs.size())
      {
         mBlogs.remove(index);
         if (saveBlogToFile() == false)
         {
            refreshData();
            return false;
         }
         return true;
      }
      return false;
   }

   private void refreshData()
   {
      mBlogs.clear();
      openBlogFromFile();
   }

   public int getMaxBlogElements()
   {
      return mBlogs.size();
   }

   public BlogElement getBlogElement(int index)
   {
      if (index < mBlogs.size())
         return mBlogs.get(index);
      return null;
   }

   /* This parses the kml file, adding all the data to our class data structure mBlogs.
    * Note only files created by this app can be parsed (v limited).
    * We also ignore the lines at the end of the file - these will be recreated when
    * we save anyway.  Example KML:
      <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
      <kml>
        <Document xmlns="http://www.opengis.net/kml/2.2">
          <Placemark>
            <name>My First TravelBlog Post</name>
            <description>01/06/2011 21:16
               This is fun!</description>
            <Point>
              <coordinates>-1.8266775000000002,52.8473925,0</coordinates>
            </Point>
            <TimeStamp>
              <when>2011-06-01T09:16:50Z</when>
            </TimeStamp>
          </Placemark>
        </Document>
      </kml>    
    */
   private Boolean openBlogFromFile()
   {
      // Dom it up
      try
      {
         File file = new File(Environment.getExternalStorageDirectory() + mPath
               + "/" + mFile);

         if (file.exists() == false)
            return false;
         // Create instance of DocumentBuilderFactory
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

         // Get the DocumentBuilder
         DocumentBuilder docBuilder = factory.newDocumentBuilder();

         Document dom = docBuilder.parse(file);
         Element root = dom.getDocumentElement();
         NodeList placemarks = root.getElementsByTagName("Placemark");
         for (int i = 0; i < placemarks.getLength(); i++)
         {
            BlogElement blog = new BlogElement();
            Node placemark = placemarks.item(i);
            NodeList properties = placemark.getChildNodes();
            Boolean foundPoint = false;
            int j;
            for (j = 0; j < properties.getLength(); j++)
            {
               Node property = properties.item(j);
               String name = property.getNodeName();
               if (name.equalsIgnoreCase("Point"))
               {
                  foundPoint = true;
                  break;
               }
            }
            if (foundPoint == false)
            {
               continue;
            }

            for (j = 0; j < properties.getLength(); j++)
            {
               Node property = properties.item(j);
               String name = property.getNodeName();
               if (name.equalsIgnoreCase("name"))
               {
                  blog.name = property.getFirstChild().getNodeValue();
               }
               else if (name.equalsIgnoreCase("description"))
               {
                  StringBuilder text = new StringBuilder();
                  NodeList chars = property.getChildNodes();
                  for (int k = 0; k < chars.getLength(); k++)
                  {
                     text.append(chars.item(k).getNodeValue());
                  }
                  blog.description = text.toString();
               }
               else if (name.equalsIgnoreCase("Point"))
               {
                  NodeList pointProperties = property.getChildNodes();
                  for (int k = 0; k < pointProperties.getLength(); k++)
                  {
                     Node pointProperty = pointProperties.item(k);
                     if (pointProperty.getNodeName().equalsIgnoreCase(
                           "coordinates"))
                     {
                        blog.location = (pointProperty.getFirstChild()
                              .getNodeValue());
                     }
                  }
               }
               else if (name.equalsIgnoreCase("TimeStamp"))
               {
                  NodeList pointProperties = property.getChildNodes();
                  for (int k = 0; k < pointProperties.getLength(); k++)
                  {
                     Node pointProperty = pointProperties.item(k);
                     if (pointProperty.getNodeName().equalsIgnoreCase("when"))
                     {
                        blog.timeStamp = (pointProperty.getFirstChild()
                              .getNodeValue());
                     }
                  }
               }
            }
            mBlogs.add(blog);
         }

      }
      catch (Exception e)
      {
         Log.e("TRAVEL_DEBUG", "DOM Parse error", e);
      }
      return true;
   }

   /* Only used for the overall trip stats - rudimentary stuff */
   public float getTotalDistance()
   {
      float total = 0.0F;
      Double previousLon = 0.0;
      Double previousLat = 0.0;
      Boolean previousLocValid = false;
      for (int i = 0; i < mBlogs.size(); i++)
      {
         BlogElement blog = (BlogElement) mBlogs.get(i);
         String[] temp;
         Double lat, lon;
         try
         {
            temp = blog.location.split(",");
            if (temp.length < 2)
            {
               continue;
            }
            lon = Double.parseDouble(temp[0]);
            lat = Double.parseDouble(temp[1]);
         }
         catch (Exception e)
         {
            continue;
         }

         if (previousLocValid == true)
         {
            float results[] = { 0, 0, 0, 0, 0 };
            Location.distanceBetween(previousLat, previousLon, lat, lon, results);
            total += results[0];
            // Log.d("TRAVEL_DEBUG", "Distance: "+ Math.round(results[0]) +
            // "m");
         }

         previousLocValid = true;
         previousLon = lon;
         previousLat = lat;
      }

      return total;
   }

   /* here we run through the mBlogs data structure and save to XML */
   private Boolean saveBlogToFile()
   {
      String path = Environment.getExternalStorageDirectory() + mPath + "/"
            + mFile;
      File newxmlfile = new File(path);
      try
      {
         newxmlfile.createNewFile();
      }
      catch (IOException e)
      {
         Log.e("IOException", "exception in createNewFile() method");
         return false;
      }
      // we have to bind the new file with a FileOutputStream
      FileOutputStream fileos = null;
      try
      {
         fileos = new FileOutputStream(newxmlfile);
      }
      catch (FileNotFoundException e)
      {
         Log.e("FileNotFoundException", "can't create FileOutputStream");
         return false;
      }
      // we create a XmlSerializer in order to write xml data
      XmlSerializer serializer = Xml.newSerializer();
      try
      {

         // we set the FileOutputStream as output for the serializer, using
         // UTF-8 encoding
         serializer.setOutput(fileos, "UTF-8");
         // Write <?xml declaration with encoding (if encoding not null) and
         // standalone flag (if standalone not null)
         serializer.startDocument(null, Boolean.valueOf(true));
         // set indentation option
         serializer.setFeature(
               "http://xmlpull.org/v1/doc/features.html#indent-output", true);
         // start a tag called "root"
         serializer.startTag(null, "kml");
         serializer.startTag(null, "Document");
         // set an attribute called "xmlns" with a "http:..." for <kml>
         serializer.attribute(null, "xmlns", "http://www.opengis.net/kml/2.2");
         /* Perhaps use for icon in future?:
          * <Style id="desired_id"> <IconStyle> <Icon>
          * <href>http://www.yourwebsite.com/your_preferred_icon.png</href>
          * <scale>1.0</scale> </Icon> </IconStyle> </Style> then in placemark:
          * <styleUrl>#desired_id</styleUrl>
          */
         for (int j = 0; j < 2; j++)
         {
            String previousLocation = null;
            for (int i = 0; i < mBlogs.size(); i++)
            {
               BlogElement blog = (BlogElement) mBlogs.get(i);

               if ((j == 1) && (i == 0))
               {
                  /* if we are running through this blog loop for the second time
                   * we are adding the kml stuff to draw a line from one point to 
                   * the next.  This will be in a folder that isn't open (no-one wants to see 
                   * it on the left-hand pane in Google Maps anyway):
                   * 
                   <Folder>
                     <name>Lines</name>
                     <open>0</open>
                     <Placemark>
                       <LineString>
                         <coordinates>-1.8266775000000002,52.8473925,0 -1.8266775000000002,53.8473925,0</coordinates>
                       </LineString>
                     </Placemark>
                     <Placemark>
                       <LineString>
                         <coordinates>-1.8266775000000002,53.8473925,0 -1.8266919000000001,54.847400300000004,0</coordinates>
                       </LineString>
                     </Placemark>
                   </Folder>
                   */
                  serializer.startTag(null, "Folder");
                  serializer.startTag(null, "name");
                  serializer.text("Lines");
                  serializer.endTag(null, "name");
                  serializer.startTag(null, "open");
                  serializer.text("0");
                  serializer.endTag(null, "open");
               }
               else
               {
                  serializer.startTag(null, "Placemark");
               }
               if ((blog.name != null) && (j == 0))
               {
                  serializer.startTag(null, "name");
                  // write some text inside <name>
                  serializer.text(blog.name);
                  serializer.endTag(null, "name");
               }
               if ((blog.description != null) && (j == 0))
               {
                  serializer.startTag(null, "description");

                  BufferedReader reader = new BufferedReader(new StringReader(
                        blog.description));
                  String line;
                  try
                  {
                     while ((line = reader.readLine()) != null)
                     {
                        if (line.length() > 0)
                        {
                           serializer.text(line);
                           serializer.text("\r");
                        }
                     }
                  }
                  catch (Exception e)
                  {
                     Log.e("Exception",
                           "error occurred while reading blog name");
                  }
                  serializer.endTag(null, "description");
               }
               if (blog.location != null)
               {
                  if (j == 0)
                  {
                     serializer.startTag(null, "Point");
                     serializer.startTag(null, "coordinates");
                     serializer.text(blog.location);
                     serializer.endTag(null, "coordinates");
                     serializer.endTag(null, "Point");
                  }
                  if ((previousLocation != null) && (j == 1))
                  {
                     serializer.startTag(null, "LineString");
                     serializer.startTag(null, "coordinates");
                     String coods = previousLocation + " " + blog.location;
                     serializer.text(coods);
                     serializer.endTag(null, "coordinates");
                     serializer.endTag(null, "LineString");
                  }
               }
               if (blog.timeStamp != null)
               {
                  if (j == 0)
                  {
                     /* timestamp can be used in Google Earth */
                     serializer.startTag(null, "TimeStamp");
                     serializer.startTag(null, "when");
                     serializer.text(blog.timeStamp);
                     serializer.endTag(null, "when");
                     serializer.endTag(null, "TimeStamp");
                  }

               }
               previousLocation = blog.location;
               if ((j == 0) || (i > 0))
               {
                  serializer.endTag(null, "Placemark");
               }
               if ((j == 1) && (i == (mBlogs.size() - 1)))
               {
                  serializer.endTag(null, "Folder");
               }
            }
         }
         serializer.endTag(null, "Document");
         serializer.endTag(null, "kml");
         serializer.endDocument();
         // write xml data into the FileOutputStream
         serializer.flush();
         // finally we close the file stream
         fileos.close();
      }
      catch (Exception e)
      {
         Log.e("Exception", "error occurred while creating xml file");
         return false;
      }
      return true;
   }

}

/* The Blog Element used in the array */
class BlogElement
{
   public String description;
   public String name;
   public String location;
   public String timeStamp;

   public BlogElement()
   {
      this.description = null;
      this.name = null;
      this.location = null;
      this.timeStamp = null;
   }
}