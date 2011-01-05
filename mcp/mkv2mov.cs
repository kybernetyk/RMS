using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;
using System.Text.RegularExpressions;

// recursively finds all mkvs in the current folder, extracts their audio and video tracks,
// converts the audio track to AAC, and merges them back to a .mov file

namespace mkvtomov
{
	class Program
	{
		static List<string> lstFilesFound = new List<string>();

		const string mkvinfo = "mkvinfo";
		const string mkvextract = "mkvextract";
		const string mp4box = @"mp4box";
		const string ffmpeg = @"ffmpeg";

		const string mkvinfoError = "No segment/level 0 element found.";
		const string desiredVideoCodec = "h.264";
		const string desiredAudioCodec = "AAC";
		const string containerFormat = "matroska";

		const string tmpVideo = "tmp.h264";
		const string tmpAudio = "tmp.audio";
		const string tmpAAC = "tmp.aac";

		const string outputFormat = "mov";

		static void Main(string[] args)
		{
			// search folder "unrar" for all mkv files
			DirSearch(".", "*.*");

			foreach (string file in lstFilesFound)
			{

				// delete temporary files
				if (File.Exists(tmpAAC))
					File.Delete(tmpAAC);
				if (File.Exists(tmpAudio))
					File.Delete(tmpAudio);
				if (File.Exists(tmpVideo))
					File.Delete(tmpVideo);

				string output = LaunchProcess(mkvinfo, file);
				// not an mkv file
				if (output.Contains(mkvinfoError))
				{
					Console.WriteLine(file + " is not an mkv file.");
					continue;
				}
				// does not contain an h.264 stream
				if (!output.Contains(desiredVideoCodec))
				{
					Console.WriteLine(file + " does not contain an h.264 stream.");
					continue;
				}
				// file is matroska
				if (!output.Contains(containerFormat))
				{
					Console.WriteLine(file + " appears to be of a wrong format.");
					continue;
				}

				// split tracks - this is information about video and audio. both have fps information
				// we only want information about video FPS
				string[] tracks = Regex.Split(output, "A track");
				// movie without sound, or sound without movie. WTF?
				if (tracks.Length < 2)
				{
					Console.WriteLine(file + " has less than two tracks (needs audio + video).");
					continue;
				}
				int videotrack = 0;
				bool found = false;
				for (int i = 1; i < tracks.Length; i++)
				{
					if (tracks[i].Contains(desiredVideoCodec))
					{
						videotrack = i;
						found = true;
						break;
					}
				}

				if (!found)
				{
					Console.WriteLine(file + ": desired video codec not found");
					continue;
				}

				int audiotrack = (videotrack == 1) ? 2 : 1;

				// figure out video fps for mp4box
				// (23.976 fps for a video track)
				Match m = Regex.Match(tracks[videotrack], "[0-9]+\\.[0-9]+ fps for a video track");
				// couldn't figure out fps
				if (!m.Success)
				{
					Console.WriteLine("Couldn't figure out FPS for " + file + ". " + m.ToString());
					continue;
				}

				string fps = m.ToString().Substring(0, m.ToString().IndexOf(' '));

				// find audio codec used. mp4 requires AAC.
				bool needsAudioConversion = !tracks[audiotrack].Contains(desiredAudioCodec);

				// extract both tracks
				Console.WriteLine(LaunchProcess(mkvextract, "tracks " + file + " " + audiotrack + ":" + tmpAudio + " " + videotrack + ":" + tmpVideo));

				// extraction broke
				if (!File.Exists(tmpAudio) || !File.Exists(tmpVideo))
				{
					Console.WriteLine(file + ": temporary output files were not created for some reason (split video and audio track)");
					continue;
				}

				// convert audio to AAC if something else was found
				if (needsAudioConversion)
				{
					LaunchProcess(ffmpeg, "-i " + tmpAudio + " -acodec libfaac -ab 256k " + tmpAAC);
					File.Delete(tmpAudio);
					// something went horribly wrong while converting @[
					if (!File.Exists(tmpAAC))
					{
						Console.WriteLine(file + ": audio track could not be converted to aac.");
						continue;
					}
				}
				else
					File.Move(tmpAudio, tmpAAC);    // same as rename as long as it's on the same disk

				// merge output to mov
				string outputFilename = file.Substring(0, file.LastIndexOf('.') + 1) + outputFormat;

				Console.WriteLine("-add " + tmpAAC + " -add " + tmpVideo + " -fps " + fps + " " + outputFilename);
				Console.WriteLine(LaunchProcess(mp4box, "-add " + tmpAAC + " -add " + tmpVideo + " -fps " + fps + " " + outputFilename));

				if (!File.Exists(outputFilename))
				{
					Console.WriteLine(file + ": output file " + outputFilename + " not found.");
					continue;
				}

				Console.WriteLine(file + ": oO - I think it worked!");
				File.Delete(file);
			}

			// delete temporary files
			if (File.Exists(tmpAAC))
				File.Delete(tmpAAC);
			if (File.Exists(tmpAudio))
				File.Delete(tmpAudio);
			if (File.Exists(tmpVideo))
				File.Delete(tmpVideo);
		}


		static void DirSearch(string sDir, string fType)
		{
			try
			{
				foreach (string d in Directory.GetDirectories(sDir))
				{
					foreach (string f in Directory.GetFiles(d, fType))
					{
						lstFilesFound.Add(f);
					}
					DirSearch(d, fType);
				}
			}
			catch (System.Exception excpt)
			{
				Console.WriteLine(excpt.Message);
			}
		}


		static string LaunchProcess(string name, string parameter)
		{
			string output = string.Empty;

			try
			{
				// Start the child process.
				Process p = new Process();
				// Redirect the output stream of the child process.
				p.StartInfo.UseShellExecute = false;
				p.StartInfo.RedirectStandardOutput = true;
				p.StartInfo.Arguments = parameter;
				p.StartInfo.FileName = name;
				p.Start();
				// Do not wait for the child process to exit before
				// reading to the end of its redirected stream.
				// p.WaitForExit();
				// Read the output stream first and then wait.
				output = p.StandardOutput.ReadToEnd();
				p.WaitForExit();
			}
			catch (Exception e)
			{
				Console.WriteLine(e.Message);
			}

			return output;
		}

	}
}


