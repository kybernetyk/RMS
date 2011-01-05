using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net;
using System.Text.RegularExpressions;

namespace CFA
{
	class Program
	{
		const string lockfile = "rsswatch.lock";
		const string watchlock1 = "rslinkscheck.lock";

		static void Main(string[] args)
		{
			using (File.Create(lockfile));
			System.Threading.Thread.Sleep(1000);
			if (File.Exists(watchlock1))
			{
				File.Delete(lockfile);
				Environment.Exit(1);
			}

			string matchlist = string.Empty;

			List<string> serieslist = new List<string>();
			if (File.Exists("series"))
				serieslist = ReadFile("series").Split('\n').ToList<string>();

			// load list of series to parse for
			if (!File.Exists("matchlist"))
			{
				File.Delete(lockfile);
				Environment.Exit(0);    // nothing to do
			}

			matchlist = ReadFile("matchlist");

			// parse series file. everything after "#" is a comment
			// items starting with + are mandatory
			// items starting with - are not allowed
			List<List<string>> mustMatch = new List<List<string>>();
			List<List<string>> musntMatch = new List<List<string>>();
			if (!ParseSeriesFile(matchlist, ref mustMatch, ref musntMatch))
			{
				File.Delete(lockfile);
				Environment.Exit(1);
			}

			// download RSS feed
			string rssfeed = GetURL("http://feeds.feedburner.com/rslinks-org");

			List<string> urlList = new List<string>();

			// parse
			string[] titles = Regex.Split(rssfeed, "\\<title\\>");
			foreach (string title in titles)
			{
				int end = title.LastIndexOf("</title>");
				if (end < 0)
					continue;
				string tmp = title.Substring(0, end);
				Console.WriteLine(tmp);
				bool matches = true;
				for (int i = 0; i < mustMatch.Count; i++)
				{
					foreach (string match in mustMatch[i])
						if (!tmp.Contains(match))
						{
							matches = false;
							break;
						}

					foreach (string match in musntMatch[i])
						if (tmp.Contains(match))
						{
							matches = false;
							break;
						}

					if (matches)
					{
						Console.WriteLine("found match!");
						int start = title.IndexOf("<feedburner:origLink>") + 21;
						int stop = title.IndexOf("</feedburner:origLink>") - start;
						string substring = title.Substring(start, stop);
						urlList.Add(substring);
						break;
					}

					matches = true;

				}
			}

			// add to list
			Console.WriteLine(serieslist.Count);

			foreach (string url in urlList)
				if (!serieslist.Contains(url))
					serieslist.Add(url);

			File.Delete("series");
			WriteList("series", serieslist);

			File.Delete(lockfile);

		}

		public static string GetURL(string url)
		{
			try
			{
				// used to build entire input
				StringBuilder sb = new StringBuilder();

				// used on each read operation
				byte[] buf = new byte[8192];

				// prepare the web page we will be asking for
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

				return sb.ToString();
			}
			catch (Exception e) { }

			return string.Empty;
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

		public static bool ParseSeriesFile(string seriesfile, ref List<List<string>> must, ref List<List<string>> mustnt)
		{
			string[] series = seriesfile.Split(System.Environment.NewLine.ToCharArray(), StringSplitOptions.RemoveEmptyEntries);
			foreach(string line in series)
			{
				List<string> mustArray = new List<string>();
				List<string> musntArray = new List<string>();

				must.Add(mustArray);
				mustnt.Add(musntArray);

				string tmp = line;

				// remove 'comments'
				int commentPosition = line.IndexOf('#');
				if (commentPosition > -1)
					tmp = line.Substring(0, commentPosition);

				// find stuff that starts with +/- and add that to "must"; anything between + and +/-/EOL is added
				while (tmp.Length > 0)
				{
					int nextPlus = tmp.IndexOf('+', 1);
					int nextMinus = tmp.IndexOf('-', 1);
					int nextCharacter = 0;
					if (nextMinus < 0)
						nextCharacter = nextPlus;
					else if (nextPlus < 0)
						nextCharacter = nextMinus;
					else
						nextCharacter = (nextPlus > nextMinus) ? nextMinus : nextPlus;

					if (nextCharacter < 0)
						nextCharacter = tmp.Length + 1;

					string addString = tmp.Substring(1, nextCharacter - 2);

					if (tmp[0] == '+') // + is closest
						mustArray.Add(addString);
					else if (tmp[0] == '-')
						musntArray.Add(addString);
					else // crash
					{
						Console.WriteLine("series in wrong format. crashing nao!");
						tmp = string.Empty;
						return false;
					}

					tmp = tmp.Substring(Math.Min(nextCharacter, tmp.Length));
				}
			}
			return true;
		}

	}



}

