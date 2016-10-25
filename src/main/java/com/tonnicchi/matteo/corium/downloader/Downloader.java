package com.tonnicchi.matteo.corium.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.tonnicchi.matteo.corium.xpathextractor.XPathExtractor;

public class Downloader {

	private static final String[] EXCLUDED_TAGS = new String[] { "style", "script", "img" };

	private static final File UNREACHABLE_URLS_LOG = new File("scraped/unreachable_urls.log");
	private static final String USER_AGENT = "matteo.tonnicchi@gmail.com";
	public static final String SCRAPED_DIR_PATH = "scraped";
	public static final String CLEANED_DIR_PATH = "cleaned";
	public static final String REGEXES_DIR_PATH = "regexes";

	public static String downloadLazy(String sourceKey, String address) {
		
		try {

			return load(CLEANED_DIR_PATH, sourceKey, address);

		} catch (FileNotFoundException e) {
			
			System.out.println("NOT FOUND ");

			return downloadSaveReturn(sourceKey, address);

		} catch (IOException e) {
			throw new IllegalStateException("Can not read file", e);
		}

	}

	private static String downloadSaveReturn(String sourceKey, String address) {

		String downloadedFile = download(USER_AGENT, address);

		if(!downloadedFile.isEmpty()){
			String cleaned = cleanHTML(downloadedFile);
			writeFile(SCRAPED_DIR_PATH, sourceKey, address, downloadedFile);
			writeFile(CLEANED_DIR_PATH, sourceKey, address, cleaned);
			writeFile(REGEXES_DIR_PATH, sourceKey, sourceKey, "");
			return cleaned;
		} else {
			updateUnreachableUrls(address);
		}
		

		return downloadedFile;
	}

	public static String download(String userAgent, String address) {

		if (isKnownUnreachable(address)) {
			return "";
		}

		System.setProperty("http.agent", userAgent);

		try(InputStream is = toURL(address).openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
			return br.lines().collect(Collectors.joining("\n"));
		} catch (FileNotFoundException fnfe) {
			updateUnreachableUrls(address);
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return "";

	}

	private static URL toURL(String address) {
		try {
			return new URL(address);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Can not create a URL from "+address);
		}
	}

	private static void updateUnreachableUrls(String address) {
		try {
			if (!isKnownUnreachable(address)) {
				Files.append(address + "\n", UNREACHABLE_URLS_LOG, Charsets.UTF_8);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not update unreachable urls log file", e);
		}
	}

	public static boolean isKnownUnreachable(String address) {
		try {
			return Files.readLines(UNREACHABLE_URLS_LOG, Charsets.UTF_8).contains(address);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read unreachable urls log file", e);
		}
	}

	public static String load(String root, String sourceKey, String address) throws IOException {
		String resourcePath = composeDestinationPathFor(root, sourceKey, address);
		System.out.println("LOAD: "+resourcePath);
		return Files.toString(new File(resourcePath), Charsets.UTF_8);
	}

	private static String composeDestinationPathFor(String root, String sourceKey, String address) {
		try {
			return composeSourceFolder(root, encode(sourceKey)) + File.separator + encodeAsFileName(address);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Can not encode the destination path ", e);
		}
	}

	private static String composeSourceFolder(String root, String sourceKey) {
		try {
			return root + File.separator + URLEncoder.encode(sourceKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Can not encode the source folder path ", e);
		}
	}

	public static List<String> testRegexOnAllFilesIn(String regex, String sourceKey) {
		final List<String> results = new ArrayList<>();
		try {
			java.nio.file.Files.walkFileTree(Paths.get(composeSourceFolder(SCRAPED_DIR_PATH, sourceKey)),
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							results.addAll(
									XPathExtractor.extract(regex, Files.toString(file.toFile(), Charsets.UTF_8)));
							return FileVisitResult.CONTINUE;
						}
					});
		} catch (IOException e) {
			throw new IllegalStateException("Error while handling " + regex + " on " + sourceKey, e);
		}
		return results;
	}


	public static void writeFile(String root, String sourceKey, String address, String content) {
		String path = composeDestinationPathFor(root, sourceKey, address);
		try {
			File file = new File(path);
			Files.createParentDirs(file);
			Files.write(content, file, Charsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Error while writing file to " + path, e);
		}
	}

	private static String encode(String folderName) {
		try {
			return URLEncoder.encode(folderName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Can not encode the folder name " + folderName, e);
		}
	}

	private static String encodeAsFileName(String address) throws UnsupportedEncodingException {
		String encoded
			= URLEncoder.encode(address.replaceFirst("http://", ""), "UTF-8")
				.replaceAll("%2F", File.separator);
		int lastSeparatorIndex = encoded.lastIndexOf(File.separator);
		encoded = lastSeparatorIndex == -1 ? encoded : encoded.substring(lastSeparatorIndex+1);
		return encoded + ".html";
	}

	private static String cleanHTML(String html) {
		String body = Jsoup.parse(html).select("body").toString();		
		Whitelist whitelist = Whitelist.relaxed();
		whitelist.removeTags(EXCLUDED_TAGS);		
		return "<html><body>\n"+Jsoup.clean(body, whitelist)+"\n</body></html>";
	}

}
