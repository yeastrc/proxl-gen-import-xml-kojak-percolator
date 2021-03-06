package org.yeastrc.proxl.proxl_gen_import_xml_kojak.kojak.core_entry_point;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.builder.MatchedProteinsBuilder;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.builder.MatchedProteins_IsotopeLabel_Param;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.command_line_options_container.CommandLineOptionsContainer;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.constants.IsotopeLabelValuesConstants;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.exceptions.ProxlGenXMLDataException;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.is_monolink.IsModificationAMonolink;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.isotope_labeling.Isotope_Labels_SpecifiedIn_KojakConfFile;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.kojak.KojakConfFileReaderResult;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.kojak.ProcessKojakConfFile;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.kojak.main.AddKojakCutoffOnImport;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.kojak.main.ProcessKojakFileOnly;
import org.yeastrc.proxl_import.api.xml_dto.Linker;
import org.yeastrc.proxl_import.api.xml_dto.Linkers;
import org.yeastrc.proxl_import.api.xml_dto.ProxlInput;
import org.yeastrc.proxl_import.api.xml_dto.SearchProgramInfo;
import org.yeastrc.proxl_import.api.xml_dto.SearchPrograms;
import org.yeastrc.proxl_import.create_import_file_from_java_objects.main.CreateImportFileFromJavaObjectsMain;

/**
 * This is the internal core entry point to generating the import XML from Kojak data
 *
 */
public class GenImportXMLFromKojakDataCoreEntryPoint {


	private static final Logger log = Logger.getLogger( GenImportXMLFromKojakDataCoreEntryPoint.class );
	

	/**
	 * private constructor
	 */
	private GenImportXMLFromKojakDataCoreEntryPoint() { }

	public static GenImportXMLFromKojakDataCoreEntryPoint getInstance() {
		
		return new GenImportXMLFromKojakDataCoreEntryPoint();
	}
	
	

	
	/**
	 * @param fastaFileWithPathFileFromCmdLine
	 * @param linkerNamesStringsList
	 * @param searchName
	 * @param proteinNameDecoyPrefix
	 * @param monolinkModificationMasses
	 * @param skipPopulatingMatchedProteins
	 * @param forceDropKojakDuplicateRecordsOptOnCommandLine
	 * @param kojakOutputFile
	 * @param kojakConfFile
	 * @param outputFile
	 * @throws Exception
	 */
	public void doGenFile( 
			
			File fastaFileWithPathFileFromCmdLine,
			List<String> linkerNamesStringsList,
			String searchName,
			String proteinNameDecoyPrefix,
			
			Set<BigDecimal> monolinkModificationMasses,

			boolean skipPopulatingMatchedProteins,
			


			BigDecimal scoreCutoffOnImport,

			
			boolean forceDropKojakDuplicateRecordsOptOnCommandLine,

			File kojakOutputFile,
			File kojakConfFile,
			
			File outputFile
			
			) throws Exception {
		
		if ( monolinkModificationMasses != null ) {

			IsModificationAMonolink.getInstance().setMonolinkModificationMasses( monolinkModificationMasses );
		}
		
		
		//  The object graph that will be serialized to generate the import XML file 
		
		ProxlInput proxlInputRoot = new ProxlInput();
		
		
//		proxlInputRoot.setComment(  );
		
		proxlInputRoot.setName( searchName );
		
		SearchProgramInfo searchProgramInfo = new SearchProgramInfo();
		proxlInputRoot.setSearchProgramInfo( searchProgramInfo );
		
		AddKojakCutoffOnImport.getInstance().addCutoffsOnImport( scoreCutoffOnImport, searchProgramInfo );

		
		SearchPrograms searchPrograms = new SearchPrograms();
		searchProgramInfo.setSearchPrograms( searchPrograms );
		
		Linkers linkers = new Linkers();
		proxlInputRoot.setLinkers( linkers );

		List<Linker> linkerList = linkers.getLinker();

		for ( String linkerNameString : linkerNamesStringsList ) {

			Linker linker = new Linker();
			linkerList.add( linker );

			linker.setName( linkerNameString );
		}

		//  TODO  Add more info to Linker??

		try {

			if ( forceDropKojakDuplicateRecordsOptOnCommandLine ) {

				CommandLineOptionsContainer.setForceDropKojakDuplicateRecordsOptOnCommandLine(true);
			}
			
			
			if ( kojakOutputFile == null ) {
				
				String msg = "kojakOutputFile cannot be null";
				log.error( msg );
				
				throw new IllegalArgumentException(msg);
			}
			

			if ( kojakConfFile == null ) {
				
				String msg = "kojakConfFile cannot be null";
				log.error( msg );
				
				throw new IllegalArgumentException(msg);
			}

			
			KojakConfFileReaderResult kojakConfFileReaderResult =
					ProcessKojakConfFile.getInstance().processKojakConfFile( kojakConfFile, proxlInputRoot );
			
			Isotope_Labels_SpecifiedIn_KojakConfFile isotopes_SpecifiedIn_KojakConfFile = kojakConfFileReaderResult.getIsotopes_SpecifiedIn_KojakConfFile();

			Collection<String> decoyIdentificationStringList = kojakConfFileReaderResult.getDecoyIdentificationStringFromConfFileList();
			
			if ( StringUtils.isNotEmpty( proteinNameDecoyPrefix ) ) {
				
				//  Decoys provided on command line so Override decoys from conf file 

				decoyIdentificationStringList = new ArrayList<>( 1 );
				
				decoyIdentificationStringList.add( proteinNameDecoyPrefix );
			}
			
			
			File fastaFileWithPathFile = null;
			

			if ( fastaFileWithPathFileFromCmdLine != null ) {
				
				//  fasta file provided on command line so Override fasta file from conf file 
				
				String fastaFilename = fastaFileWithPathFileFromCmdLine.getName();

				proxlInputRoot.setFastaFilename( fastaFilename );
				
				fastaFileWithPathFile = fastaFileWithPathFileFromCmdLine;

			} else {
				
				if ( kojakConfFileReaderResult.getFastaFile() == null ) {
					
					//  fasta file not provided in conf file or command line, throw error

					String msg = "No Fasta file provided by either conf file or command line.";
					log.error( msg );
					throw new ProxlGenXMLDataException(msg);
				}
				
				if ( ! kojakConfFileReaderResult.getFastaFile().exists() ) {

					//  fasta file in conf file not found, throw error

					String msg = "Fasta file in conf file does not exist: " + kojakConfFileReaderResult.getFastaFile().getCanonicalPath();
					log.error( msg );
					throw new ProxlGenXMLDataException(msg);
				}
				
				fastaFileWithPathFile = kojakConfFileReaderResult.getFastaFile();
				

				String fastaFilename = fastaFileWithPathFile.getName();

				proxlInputRoot.setFastaFilename( fastaFilename );
				
			}
			

			//  Set of protein name strings from Kojak data, null if not returning any
			Set<String> proteinNameStrings = null;
			
			if ( ! skipPopulatingMatchedProteins ) {
				proteinNameStrings = new HashSet<>();
			}
			
			
			ProcessKojakFileOnly.getInstance().processKojakFile( kojakOutputFile, proxlInputRoot, proteinNameStrings, kojakConfFileReaderResult );

			
			List<MatchedProteins_IsotopeLabel_Param> isotopeLabels_MatchedProteinsParam = new ArrayList<>();
			 
			if ( isotopes_SpecifiedIn_KojakConfFile != null && isotopes_SpecifiedIn_KojakConfFile.getIsotopeLabel_15N_filter_Value() != null ) {
				// Found Isotope label in Kojak conf file so add it to the list isotopeLabels_MatchedProteinsParam
				MatchedProteins_IsotopeLabel_Param matchedProteins_IsotopeLabel_Param = new MatchedProteins_IsotopeLabel_Param();
				matchedProteins_IsotopeLabel_Param.setIsotopeLabel_ProteinNamePrefix( isotopes_SpecifiedIn_KojakConfFile.getIsotopeLabel_15N_filter_Value() );
				matchedProteins_IsotopeLabel_Param.setIsotopeLabel_ForProxlXMLFile( IsotopeLabelValuesConstants.ISOTOPE_LABEL_FOR_PROXL_XML_FILE_15N );
				isotopeLabels_MatchedProteinsParam.add( matchedProteins_IsotopeLabel_Param );
			}

			if ( ! skipPopulatingMatchedProteins ) {
				
				//  fastaFileWithPathFile from command line if specified, otherwise from Kojak.conf file 
				
				MatchedProteinsBuilder.getInstance().buildMatchedProteins( 
						proxlInputRoot, fastaFileWithPathFile, isotopeLabels_MatchedProteinsParam, kojakConfFileReaderResult.getDecoyIdentificationStringFromConfFileList() );
			}
			
			
			try {
			
				CreateImportFileFromJavaObjectsMain.getInstance().createImportFileFromJavaObjectsMain( outputFile, proxlInputRoot );
				
			} catch ( Exception e ) {
				
				String msg = "Error writing output file: " + e.toString();
				log.error( msg, e );
				
				throw new Exception(msg, e);
			}

			
			
			
		} catch ( Exception e ) {
			
			System.out.println( "Exception in processing" );
			System.err.println( "Exception in processing" );
			
			e.printStackTrace( System.out );
			e.printStackTrace( System.err );
			
			
			throw e;
		}
		
	}

}
