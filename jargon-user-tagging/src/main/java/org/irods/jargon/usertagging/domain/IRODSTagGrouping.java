/**
 *
 */
package org.irods.jargon.usertagging.domain;

import java.io.Serializable;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.query.MetaDataAndDomainData.MetadataDomain;
import org.irods.jargon.core.query.UserAnnotatedCatalogItem;

/**
 * Represents a collection of user tags for a domain object. This class
 * encapsulates the treatment of tags as one space delimited string of tag
 * values, and allows the storing, retrieval, and update of tags for a domain
 * object in iRODS based on a provided string of space-delimited tags.
 *
 * This object is a bridge between the typical client presentation of a free tag
 * area, and the underlying one-AVU-per-tag structure of the data.
 *
 * @author Mike Conway - DICE (www.irods.org)
 *
 */
public final class IRODSTagGrouping extends UserAnnotatedCatalogItem implements Serializable {

	private static final long serialVersionUID = 5739316544881647862L;
	private final String spaceDelimitedTagsForDomain;

	/**
	 * Depicts user tags for a domain object as one string of space-delimited tags,
	 * and describes the user and object that is being tagged.
	 *
	 * @param metadataDomain
	 *            {@code MetaDataAndDomainData.MetadataDomain} enum value that
	 *            identifies the domain object type that is tagged (e.g. DataObject,
	 *            Collection)
	 * @param domainUniqueName
	 *            {@code String} with the unque name for the tagged data object
	 *            (e.g. iRODS absolute path for a data object or collection).
	 * @param spaceDelimitedTagsForDomain
	 *            {@code String} with a space-delimited list of tags.
	 * @param userName
	 *            {@code String} with the user name who is associated with the tags.
	 * @throws JargonException
	 *             {@link JargonException}
	 */
	public IRODSTagGrouping(final MetadataDomain metadataDomain, final String domainUniqueName,
			final String spaceDelimitedTagsForDomain, final String userName) throws JargonException {

		super(metadataDomain, domainUniqueName, userName);

		if (spaceDelimitedTagsForDomain == null) {
			throw new JargonException("null spaceDelimitedTagsForDomain");
		}

		this.spaceDelimitedTagsForDomain = spaceDelimitedTagsForDomain.trim();

	}

	public String getSpaceDelimitedTagsForDomain() {
		return spaceDelimitedTagsForDomain;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IRODSTagGrouping:");
		sb.append("\n   metadataDomain:");
		sb.append(getMetadataDomain());
		sb.append("\n   domainUniqueName:");
		sb.append(getDomainUniqueName());
		sb.append("\n   spaceDelimitedTagsForDomain:");
		sb.append(spaceDelimitedTagsForDomain);
		sb.append("\n   userName:");
		sb.append(getUserName());
		return sb.toString();
	}

}
