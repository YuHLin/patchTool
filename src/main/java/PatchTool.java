
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 补丁扣文件工具类
 * 
 * @author wille.li
 * 
 */
public class PatchTool {

	// 源文件夹路径
	public static String SOURCE_CODE_PATH = "";

	// 目标文件夹路径
	public static String TARGET_PATCH_PATH = "";

	// 需要存放打补丁路径的配置文件，这里可以添加自己修改过的文件路径
	public static String CHANGE_FILE_PATH = "/ChangeFile.properties";

	public static String BI_WEB = "bi-report-web";

	private static String RESOURCE = "resources";

	private static String JAVA = "java";

	private static String WEBAPP = "webapp";

	/**
	 * 斜杠
	 */
	private static String SLASH = "/";
	
	private static void initPath(){
		// 保持斜杠一致
		String path = System.getProperty("user.dir").replace("\\", SLASH);
		SOURCE_CODE_PATH = path;
		TARGET_PATCH_PATH = path + "/patches";
		System.out.println("--------------- 初始化目录 ------------------");
		System.out.println("当前目录：" + SOURCE_CODE_PATH);
		System.out.println("补丁文件存放目录: " + TARGET_PATCH_PATH);
		System.out.println("--------------- 初始化目录 ------------------");
	}

	public static void main(String[] args) throws Exception {
		initPath();
		File file = new File(TARGET_PATCH_PATH);// 如果目标文件夹不存在就生成一个，例如不小心删除的
		if (!file.exists()) {
			file.mkdirs();
		}
		List<String> targetlist = analyse();// 分析配置文件循环获取路径
		for (int i = 0; i < targetlist.size(); i++) {
			String targetpath = targetlist.get(i);
			System.out.println("目标目录:" + targetpath);
			String cleanpath = cleanPath(targetpath);
			try {
				copy(cleanpath, targetpath);
			} catch (Exception e) {
				System.out.println("patch失败：" + TARGET_PATCH_PATH + targetpath);
				System.out.println("请检查文件目录。");
				return;
			}
		}
		System.out.println("-------------- patch完毕！---------------");
	}

	/**
	 * 分析项目路径，把java文件目录映射到class文件目录
	 * 
	 * @return
	 * @throws Exception
	 */
	public static List<String> analyse() throws Exception {
		List<String> list = new ArrayList<String>();
		Properties prop = new Properties();
		InputStream in = new FileInputStream(SOURCE_CODE_PATH
				+ CHANGE_FILE_PATH);
		prop.load(in);
		Set<Object> set = prop.keySet();
		for (Object obj : set) {
			String path = obj.toString();
			String[] paths = path.split(SLASH);
			if (!"".equals(paths[0])) {
				System.out.println("存在数据路径配置问题，请检查！");
				return list;
			}
			String newPath = path;
			StringBuffer temPath = new StringBuffer(20);
			String projectName = paths[1];
			String pathName = paths[4]; // java 或者 resource 或者 webapp
			// 1. resource目录下面的 直接替换
			if (JAVA.equals(pathName) && BI_WEB.equals(projectName)) {
				temPath.append(SLASH).append(projectName);
				temPath.append("/WEB-INF/classes");
				for (int i = 5; i < paths.length; i++) {
					temPath.append(SLASH).append(paths[i]);
				}
				newPath = temPath.toString().replace(".java", ".class");
			} else if (JAVA.equals(pathName) || RESOURCE.equals(pathName)) {
				temPath.append(SLASH).append(projectName);
				for (int i = 5; i < paths.length; i++) {
					temPath.append(SLASH).append(paths[i]);
				}
				newPath = temPath.toString().replace(".java", ".class");
			} else if (WEBAPP.equals(pathName)) {
				temPath.append(SLASH).append(projectName);
				for (int i = 5; i < paths.length; i++) {
					temPath.append(SLASH).append(paths[i]);
				}
				newPath = temPath.toString();
			}
			list.add(newPath);
		}
		return list;
	}

	/**
	 * 获取源文件目录
	 * 
	 * @param targetpath
	 * @return
	 */
	public static String cleanPath(String targetpath) {
		StringBuffer sopath = new StringBuffer(20);
		String[] paths = targetpath.split(SLASH);
		String projectName = paths[1];
		if (BI_WEB.equals(projectName)) {
			sopath.append(SLASH).append(projectName);
			sopath.append(SLASH).append("target").append(targetpath);
		} else {
			for (int i = 1; i < paths.length; i++) {
				if (i == 2) {
					sopath.append(SLASH).append("target").append(SLASH)
							.append("classes");
				}
				sopath.append(SLASH).append(paths[i]);
			}
		}

		return sopath.toString();
	}

	/**
	 * 复制源文件到patch目录
	 * 
	 * @param sofilepath
	 * @param targetfilepath
	 * @throws Exception
	 */
	public static void copy(String sofilepath, String targetfilepath)
			throws Exception {
		File sfile = new File(SOURCE_CODE_PATH + sofilepath);
		File dir = new File(TARGET_PATCH_PATH
				+ targetfilepath.substring(0, targetfilepath.lastIndexOf("/")));
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File dfile = new File(dir, ""
				+ targetfilepath.substring(targetfilepath.lastIndexOf("/"),
						targetfilepath.length()));
		if (sfile.exists()) {
			FileInputStream fis = new FileInputStream(sfile);
			FileOutputStream fos = new FileOutputStream(dfile);
			try {
				copy(fis, fos, true);
			} catch (Exception e) {
				System.out.println("复制异常");
			}
			dfile.setLastModified(sfile.lastModified());// 保留原文件的修改时间
			System.out.println("patch成功：" + TARGET_PATCH_PATH + targetfilepath);
		} else {
			System.out.println("文件不存在！");
		}
	}

	private PatchTool() {
	}
	
	// ------------------ 以下代码是 commons-fileupload 里面复制文件的代码

	public static long copy(InputStream inputStream, OutputStream outputStream,
			boolean closeOutputStream) throws IOException {
		return copy(inputStream, outputStream, closeOutputStream,
				new byte[8192]);
	}

	public static long copy(InputStream inputStream, OutputStream outputStream,
			boolean closeOutputStream, byte[] buffer) throws IOException {
		OutputStream out = outputStream;
		InputStream in = inputStream;
		try {
			long total = 0L;
			int res;
			while (true) {
				res = in.read(buffer);
				if (res == -1) {
					break;
				}
				if (res > 0) {
					total += res;
					if (out != null) {
						out.write(buffer, 0, res);
					}
				}
			}
			if (out != null) {
				if (closeOutputStream)
					out.close();
				else {
					out.flush();
				}
				out = null;
			}
			in.close();
			in = null;
			return total;
		} finally {
			closeQuietly(in);
			if (closeOutputStream)
				closeQuietly(out);
		}
	}

	public static void closeQuietly(OutputStream output) {
		closeQuietly((Closeable) output);
	}

	/**
	 * Unconditionally close a <code>Closeable</code>.
	 * <p>
	 * Equivalent to {@link Closeable#close()}, except any exceptions will be
	 * ignored. This is typically used in finally blocks.
	 * <p>
	 * Example code:
	 * 
	 * <pre>
	 * Closeable closeable = null;
	 * try {
	 * 	closeable = new FileReader(&quot;foo.txt&quot;);
	 * 	// process closeable
	 * 	closeable.close();
	 * } catch (Exception e) {
	 * 	// error handling
	 * } finally {
	 * 	closeQuietly(closeable);
	 * }
	 * </pre>
	 * 
	 * @param closeable
	 *            the object to close, may be null or already closed
	 * @since 2.0
	 */
	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}
}
