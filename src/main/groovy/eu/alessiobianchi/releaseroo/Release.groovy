package eu.alessiobianchi.releaseroo

import org.jetbrains.annotations.NotNull

import java.text.SimpleDateFormat

class Release implements Comparable<Release> {

	public static final String DATE_FORMAT = 'yyyyMMdd_HHmmss'

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	private File file

	private String profile
	private String versionName
	private String versionCode
	private Date buildDate

	Release(File file, String profile, String versionName, String versionCode, Date buildDate) {
		this.file = file
		this.profile = profile
		this.versionName = versionName
		this.versionCode = versionCode
		this.buildDate = buildDate
	}

	File getFile() {
		return file
	}

	String getFileName() {
		return file.name
	}

	String getProfile() {
		return profile
	}

	String getVersionName() {
		return versionName
	}

	String getVersionCode() {
		return versionCode
	}

	Date getBuildDate() {
		return buildDate
	}

	String getPrintableBuildDate() {
		return dateFormat.format(buildDate)
	}

	@Override
	String toString() {
		return "Release{file=$file, profile='$profile', versionName='$versionName', versionCode='$versionCode', buildDate=$buildDate}"
	}

	@Override
	int compareTo(@NotNull Release o) {
		return o.buildDate <=> buildDate
	}
}
