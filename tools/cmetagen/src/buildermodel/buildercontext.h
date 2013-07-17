#ifndef __BUILDERCONTEXT_H
#define __BUILDERCONTEXT_H

#include "builderitem.h"

#include "cpgf/gscopedptr.h"

#include "Poco/Path.h"

#include <string>
#include <vector>

namespace metagen {


class CppContext;
class CppContanier;
class CppFile;
class BuilderFile;
class BuilderContainer;
class BuilderSectionList;
class Project;

class BuilderContext
{
public:
	typedef std::vector<BuilderItem *> ItemListType;

public:
	BuilderContext(const Project * project, const std::string & sourceFileName);
	~BuilderContext();

	void process(const CppContext * cppContext);
	
	const Project * getProject() const { return this->project; }
	const std::string & getSourceBaseFileName() const { return this->sourceBaseFileName; }
	ItemListType * getItemList() { return &this->itemList; }
	BuilderSectionList * getSectionList() { return this->sectionList.get(); }

private:
	void doProcessFile(const CppFile * cppFile);
	void generateCodeSections();

	void flatten(BuilderFile * file);
	void doFlatten(BuilderFile * file, BuilderContainer * builderContainer);

	bool shouldSkipItem(const CppItem * cppItem);

	BuilderItem * createItem(const CppItem * cppItem);

private:
	const Project * project;
	Poco::Path sourceFileName;
	std::string sourceBaseFileName;
	ItemListType itemList;
	cpgf::GScopedPointer<BuilderSectionList> sectionList;
};


} // namespace metagen


#endif
