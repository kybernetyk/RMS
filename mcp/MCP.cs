using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net;
using System.IO;
using System.Text.RegularExpressions;

namespace MCP
{
    class Program
    {
        const string addSeriesFileName = "addseries";
        const string seriesFileName = "series";
        const string downloadedFileName = "downloaded";

        static void Main(string[] args)
        {
            // load series that should be added to our list. the reason is to mark all aired episodes as already downloaded
            List<string> newurls = null;

            // contains a list of all files that need to be downloaded
            List<string> downloadList = new List<string>();

            if (File.Exists(addSeriesFileName))
                newurls = ReadFile(addSeriesFileName).Split('\n').ToList<string>();

            // fetch URLs to check from configuration file
            List<string> urls = new List<string>();
            if (File.Exists(seriesFileName))
                urls = ReadFile(seriesFileName).Split('\n').ToList<string>();

            // fetch databank that has all files ever downloaded
            List<string> downloaded = new List<string>();
            if (File.Exists(downloadedFileName))
                downloaded = ReadFile(downloadedFileName).Split('\n').ToList();
            // ignore files from newly added tv series
            if (newurls != null)
                downloaded.AddRange(GetLinks(newurls));

            // retreive all hotfile URLs from all pages we have added
            List<string> hosterURLs = GetLinks(urls);
            foreach (string url in hosterURLs)
            {
                if (!downloaded.Contains(url))
                    downloadList.Add(url);
            }

            if (newurls != null)
                urls.AddRange(newurls);

            // issue download command
            foreach (string download in downloadList)
            {
                downloaded.Add(download);
            }

            // unpack

            // mkv->mov

            // save downloaded files and update list of series to 'watch'
            File.Delete(downloadedFileName);
            WriteList(downloadedFileName, downloaded);
            File.Delete(seriesFileName);
            WriteList(seriesFileName, urls);
            File.Delete(addSeriesFileName);

        }

        public static string GetURL(string url)
        {
            // used to build entire input
            StringBuilder sb = new StringBuilder();

            // used on each read operation
            byte[] buf = new byte[8192];

            // prepare the web page we will be asking for
            try
            {
                HttpWebRequest request = (HttpWebRequest)
                    WebRequest.Create(url);
                // execute the request
                HttpWebResponse response = (HttpWebResponse)
                    request.GetResponse();

                // we will read data via the response stream
                Stream resStream = response.GetResponseStream();

                string tempString = null;
                int count = 0;

                do
                {
                    // fill the buffer with data
                    count = resStream.Read(buf, 0, buf.Length);

                    // make sure we read some data
                    if (count != 0)
                    {
                        // translate from bytes to ASCII text
                        tempString = Encoding.ASCII.GetString(buf, 0, count);

                        // continue building the string
                        sb.Append(tempString);
                    }
                }
                while (count > 0); // any more data to read?
            }
            catch (Exception e)
            {
                return null;
            }



            return sb.ToString();
        }

        public static List<string> GetLinks(List<string> urls)
        {
            // fetch all result pages for these URLs at once
            List<string> pages = new List<string>();
            foreach (string url in urls)
            {
                string tmp = GetURL(url);
                if (tmp != null)
                    pages.Add(tmp);
            }

            List<string> hosterFiles = new List<string>();

            // extract all hotfile URLs from those pages
            // ex: http://hotfile.com/dl/94092868/dfce1e4/311zamok.part1.rar
            foreach (string page in pages)
            {
                MatchCollection matches = Regex.Matches(page, "http://hotfile\\.com/dl/[0-9]+/.*?/.*?rar", RegexOptions.Compiled | RegexOptions.IgnoreCase);
                foreach (Match match in matches)
                    if (!hosterFiles.Contains(match.ToString()))
                    {
                        hosterFiles.Add(match.ToString());
                    }
            }

            return hosterFiles;
        }

        public static string ReadFile(string filename)
        {
            StreamReader streamReader = new StreamReader(filename);
            string text = streamReader.ReadToEnd();
            streamReader.Close();
            return text;
        }

        public static bool WriteList(string filename, List<string> entries)
        {
            List<string> doubleEntries = new List<string>();
            TextWriter tw = new StreamWriter(filename, false);
            foreach (string entry in entries)
                if (entry.Length > 10 && !doubleEntries.Contains(entry))
                {
                    tw.WriteLine(entry);
                    doubleEntries.Add(entry);
                }
            tw.Close();
            return true;
        }
    }
}

